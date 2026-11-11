package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.AvatarUploadDto;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.AvatarImageType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AvatarUploadRegistry;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageObjectInfo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageStorageGateway;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageUploadTicket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 头像上传应用服务（SEC-CIAM-DSN-CR-004 §5.1、§5.2）。
 * <p>
 * 职责：
 * <ul>
 *   <li>申请头像上传：前置校验、生成 avatar-{userId}.{ext}、薄封装 EDD-OSS、登记归属；</li>
 *   <li>完成确认：校验归属与有效期、调用 OSS 确认、写入 avatar_url、按幂等返回；</li>
 *   <li>失败一律 fail-closed，不产生不可用的头像地址，并写入审计。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarUploadAppService {

    private final UserProfileAppService userProfileAppService;
    private final ImageStorageGateway imageStorageGateway;
    private final AvatarUploadRegistry avatarUploadRegistry;
    private final AuditLogger auditLogger;

    /**
     * 功能开关，默认开启；关闭时两端点统一不可用
     */
    @Value("${ciam.avatar.upload.enabled:true}")
    private boolean uploadEnabled;

    /**
     * 归属登记 TTL，默认 960 秒（预签名 900 秒 + 60 秒余量）
     */
    @Value("${ciam.avatar.upload.registry-ttl-seconds:960}")
    private long registryTtlSeconds;

    /**
     * 申请头像上传。
     * <p>
     * 前置校验为低成本快速失败，格式与大小的权威校验仍在 EDD-OSS。
     *
     * @param userId           用户业务唯一标识
     * @param originalFilename APP 侧原始文件名，仅用于快速失败校验
     * @param contentType      声明 MIME
     * @param size             声明字节数
     * @return 上传凭证
     */
    public AvatarUploadDto requestUpload(String userId, String originalFilename, String contentType, Long size) {
        requireEnabled();
        AvatarImageType.validateClientFilename(originalFilename, contentType);
        long declaredSize = AvatarImageType.requireSize(size);
        String ossFilename = AvatarImageType.buildOriginalFilename(userId, contentType);

        ImageUploadTicket ticket = imageStorageGateway.generateUpload(ossFilename, contentType, declaredSize);
        avatarUploadRegistry.bind(ticket.getObjectId(), userId, registryTtlSeconds);

        logAudit(userId, AuditEventType.AVATAR_UPLOAD_REQUESTED, true);
        log.info("头像上传申请成功: userId={}, objectId={}, size={}", userId, ticket.getObjectId(), declaredSize);
        return AvatarUploadDto.builder()
                .uploadId(ticket.getObjectId())
                .uploadUrl(ticket.getUploadUrl())
                .httpMethod(ticket.getHttpMethod())
                .contentType(ticket.getContentType())
                .expiresAt(ticket.getExpiresAt())
                .build();
    }

    /**
     * 完成头像上传确认。
     *
     * @param userId   用户业务唯一标识
     * @param uploadId 上传任务标识
     * @return 已生效的头像信息
     */
    public AvatarResultDto completeUpload(String userId, String uploadId) {
        requireEnabled();

        String owner = avatarUploadRegistry.findOwner(uploadId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.AVATAR_UPLOAD_NOT_FOUND));
        if (!owner.equals(userId)) {
            log.warn("头像上传任务归属不匹配: userId={}, objectId={}", userId, uploadId);
            throw new BusinessException(CiamErrorCode.AVATAR_UPLOAD_FORBIDDEN);
        }

        Optional<ImageObjectInfo> completed = avatarUploadRegistry.findCompletion(uploadId);
        if (completed.isPresent()) {
            // 已完成确认：幂等返回，不再调用 OSS，也不重复写库
            ImageObjectInfo stored = completed.get();
            log.info("头像上传任务已确认，幂等返回: userId={}, objectId={}", userId, uploadId);
            return toResult(stored);
        }

        ImageObjectInfo object = imageStorageGateway.completeUpload(uploadId);
        userProfileAppService.updateAvatarUrl(userId, object.getAccessUrl());
        avatarUploadRegistry.markCompleted(uploadId, userId, object, registryTtlSeconds);

        logAudit(userId, AuditEventType.AVATAR_UPLOAD_COMPLETED, true);
        log.info("头像上传确认成功: userId={}, objectId={}", userId, uploadId);
        return toResult(object);
    }

    private AvatarResultDto toResult(ImageObjectInfo object) {
        return AvatarResultDto.builder()
                .avatarUrl(object.getAccessUrl())
                .objectId(object.getObjectId())
                .contentType(object.getContentType())
                .size(object.getSize())
                .build();
    }

    private void requireEnabled() {
        if (!uploadEnabled) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "头像上传功能未启用");
        }
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .build());
    }
}
