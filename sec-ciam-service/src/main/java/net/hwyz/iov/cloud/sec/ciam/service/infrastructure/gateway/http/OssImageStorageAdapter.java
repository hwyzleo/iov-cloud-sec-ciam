package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.http;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.oss.api.service.ImageService;
import net.hwyz.iov.cloud.edd.oss.api.vo.request.GenerateImageUploadRequest;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageObjectResponse;
import net.hwyz.iov.cloud.edd.oss.api.vo.response.ImageUploadResponse;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageObjectInfo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageStorageGateway;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageUploadTicket;
import org.springframework.stereotype.Component;

/**
 * EDD-OSS 图片存储网关适配器（SEC-CIAM-DSN-CR-004 §3 RD-004-3、§9.3）。
 * <p>
 * 通过 edd-oss-api 的 {@link ImageService} 调用 OSS，仅做契约映射与失败语义转换：
 * OSS 业务拒绝（400）映射为参数错误，其余失败一律 fail-closed 为图片服务不可用，
 * 不返回任何伪造凭证，也不记录完整 uploadUrl。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssImageStorageAdapter implements ImageStorageGateway {

    /**
     * EDD-OSS 业务拒绝的 HTTP 状态（图片契约校验失败）
     */
    private static final int OSS_REJECTED_STATUS = 400;

    private final ImageService imageService;

    @Override
    public ImageUploadTicket generateUpload(String originalFilename, String contentType, long size) {
        try {
            ImageUploadResponse response = imageService.generateUpload(GenerateImageUploadRequest.builder()
                    .originalFilename(originalFilename)
                    .contentType(contentType)
                    .size(size)
                    .build());
            if (response == null || StrUtil.isBlank(response.getObjectId()) || StrUtil.isBlank(response.getUploadUrl())) {
                log.error("[OSS] 申请图片上传返回不完整 objectId={}",
                        response == null ? null : response.getObjectId());
                throw new BusinessException(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE);
            }
            log.info("[OSS] 申请图片上传成功 objectId={} filename={} size={}",
                    response.getObjectId(), originalFilename, size);
            return ImageUploadTicket.builder()
                    .objectId(response.getObjectId())
                    .objectKey(response.getObjectKey())
                    .uploadUrl(response.getUploadUrl())
                    .httpMethod(response.getHttpMethod())
                    .contentType(response.getContentType())
                    .expiresAt(response.getExpiresAt())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException e) {
            throw translate(e, "申请图片上传");
        } catch (RuntimeException e) {
            log.error("[OSS] 申请图片上传异常", e);
            throw new BusinessException(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public ImageObjectInfo completeUpload(String objectId) {
        try {
            ImageObjectResponse response = imageService.completeUpload(objectId);
            if (response == null || StrUtil.isBlank(response.getAccessUrl())) {
                log.error("[OSS] 确认图片上传返回无访问地址 objectId={}", objectId);
                throw new BusinessException(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE);
            }
            log.info("[OSS] 确认图片上传成功 objectId={} status={}", objectId, response.getStatus());
            return ImageObjectInfo.builder()
                    .objectId(objectId)
                    .accessUrl(response.getAccessUrl())
                    .contentType(response.getActualContentType() == null
                            ? response.getDeclaredContentType() : response.getActualContentType())
                    .size(response.getActualSize() == null ? response.getDeclaredSize() : response.getActualSize())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (FeignException e) {
            throw translate(e, "确认图片上传");
        } catch (RuntimeException e) {
            log.error("[OSS] 确认图片上传异常 objectId={}", objectId, e);
            throw new BusinessException(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 转换 OSS 调用异常：400 视为图片契约拒绝，其余视为服务不可用。
     *
     * @param e       Feign 异常
     * @param action  操作名称，用于日志
     * @return 映射后的业务异常
     */
    private BusinessException translate(FeignException e, String action) {
        if (e.status() == OSS_REJECTED_STATUS) {
            log.warn("[OSS] {}被拒绝 status=400", action);
            return new BusinessException(CiamErrorCode.INVALID_PARAM,
                    "图片服务拒绝了本次请求，请检查图片格式、大小或文件名");
        }
        log.error("[OSS] {}失败 status={}", action, e.status());
        return new BusinessException(CiamErrorCode.IMAGE_SERVICE_UNAVAILABLE);
    }
}
