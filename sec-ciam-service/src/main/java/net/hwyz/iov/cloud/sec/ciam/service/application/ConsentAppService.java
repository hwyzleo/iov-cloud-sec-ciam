package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserConsentDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.UserConsentMapper;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserConsentRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserConsentPo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 协议同意与合规应用服务 — 编排协议同意、营销同意、撤回与数据导出删除请求。
 * <p>
 * 职责：
 * <ul>
 *   <li>记录用户协议 / 隐私政策 / 营销同意，支持版本留痕</li>
 *   <li>营销同意单独维护与撤回</li>
 *   <li>数据导出、删除请求占位，与注销流程衔接</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentAppService {

    /** 同意类型常量 */
    public static final String CONSENT_TYPE_USER_AGREEMENT = "user_agreement";
    public static final String CONSENT_TYPE_PRIVACY_POLICY = "privacy_policy";
    public static final String CONSENT_TYPE_MARKETING = "marketing";

    /** 同意状态：1=同意，0=撤回 */
    public static final int CONSENT_STATUS_AGREED = 1;
    public static final int CONSENT_STATUS_WITHDRAWN = 0;

    private final CiamUserConsentRepository consentRepository;
    private final AuditLogger auditLogger;

    /**
     * 授予同意 — 记录用户协议 / 隐私政策 / 营销同意。
     *
     * @param userId        用户 ID
     * @param consentType   同意类型：user_agreement / privacy_policy / marketing
     * @param policyVersion 协议版本
     * @param sourceChannel 来源渠道
     * @param clientType    客户端类型
     * @param operateIp     操作 IP
     * @return 创建的同意记录
     */
    public UserConsentDto grantConsent(String userId,
                                          String consentType,
                                          String policyVersion,
                                          String sourceChannel,
                                          String clientType,
                                          String operateIp) {
        validateUserId(userId);
        validateConsentType(consentType);

        UserConsentPo record = new UserConsentPo();
        record.setConsentId(UserIdGenerator.generate());
        record.setUserId(userId);
        record.setConsentType(consentType);
        record.setConsentStatus(CONSENT_STATUS_AGREED);
        record.setPolicyVersion(policyVersion);
        record.setSourceChannel(sourceChannel);
        record.setClientType(clientType);
        record.setOperateIp(operateIp);
        record.setOperateTime(DateTimeUtil.getNowInstant());
        record.setRowVersion(1);
        record.setRowValid(1);
        record.setCreateTime(DateTimeUtil.getNowInstant());
        record.setModifyTime(DateTimeUtil.getNowInstant());

        consentRepository.insert(record);

        logAudit(userId, AuditEventType.CONSENT_GRANT, true,
                "consentType=" + consentType + ",policyVersion=" + policyVersion);
        log.info("同意授予成功: userId={}, consentType={}, policyVersion={}",
                userId, consentType, policyVersion);

        return UserConsentMapper.INSTANCE.toDto(UserConsentMapper.INSTANCE.toDomain(record));
    }

    /**
     * 撤回营销同意。
     * <p>
     * 仅允许撤回 marketing 类型同意。查找最新有效的营销同意记录并将其状态置为撤回。
     *
     * @param userId    用户 ID
     * @param operateIp 操作 IP
     */
    public void withdrawMarketingConsent(String userId, String operateIp) {
        validateUserId(userId);

        List<UserConsentPo> records =
                consentRepository.findByUserIdAndConsentType(userId, CONSENT_TYPE_MARKETING);

        Optional<UserConsentPo> activeConsent = records.stream()
                .filter(r -> r.getConsentStatus() != null && r.getConsentStatus() == CONSENT_STATUS_AGREED)
                .findFirst();

        if (activeConsent.isEmpty()) {
            log.warn("未找到有效的营销同意记录: userId={}", userId);
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "未找到有效的营销同意记录");
        }

        UserConsentPo consent = activeConsent.get();
        consent.setConsentStatus(CONSENT_STATUS_WITHDRAWN);
        consent.setOperateIp(operateIp);
        consent.setOperateTime(DateTimeUtil.getNowInstant());
        consent.setModifyTime(DateTimeUtil.getNowInstant());

        consentRepository.updateByConsentId(consent);

        logAudit(userId, AuditEventType.CONSENT_WITHDRAW, true,
                "consentType=marketing,consentId=" + consent.getConsentId());
        log.info("营销同意撤回成功: userId={}, consentId={}", userId, consent.getConsentId());
    }

    /**
     * 查询用户所有同意记录。
     *
     * @param userId 用户 ID
     * @return 同意记录列表
     */
    public List<UserConsentDto> getConsentRecords(String userId) {
        validateUserId(userId);
        return consentRepository.findByUserId(userId).stream()
                .map(doObj -> UserConsentMapper.INSTANCE.toDto(UserConsentMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }

    /**
     * 按类型查询用户同意记录。
     *
     * @param userId      用户 ID
     * @param consentType 同意类型
     * @return 同意记录列表
     */
    public List<UserConsentDto> getConsentByType(String userId, String consentType) {
        validateUserId(userId);
        validateConsentType(consentType);
        return consentRepository.findByUserIdAndConsentType(userId, consentType).stream()
                .map(doObj -> UserConsentMapper.INSTANCE.toDto(UserConsentMapper.INSTANCE.toDomain(doObj)))
                .collect(Collectors.toList());
    }

    /**
     * 发起数据导出请求（占位）。
     * <p>
     * 当前仅记录审计日志，后续对接实际数据导出流程。
     *
     * @param userId 用户 ID
     */
    public void requestDataExport(String userId) {
        validateUserId(userId);

        logAudit(userId, AuditEventType.DATA_EXPORT_REQUEST, true, null);
        log.info("数据导出请求已记录: userId={}", userId);
    }

    /**
     * 发起数据删除请求（占位）。
     * <p>
     * 当前仅记录审计日志，后续与注销流程衔接处理。
     *
     * @param userId 用户 ID
     */
    public void requestDataDeletion(String userId) {
        validateUserId(userId);

        logAudit(userId, AuditEventType.DATA_DELETION_REQUEST, true, null);
        log.info("数据删除请求已记录，后续与注销流程衔接: userId={}", userId);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "userId 不能为空");
        }
    }

    private void validateConsentType(String consentType) {
        if (consentType == null || consentType.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "consentType 不能为空");
        }
        if (!CONSENT_TYPE_USER_AGREEMENT.equals(consentType)
                && !CONSENT_TYPE_PRIVACY_POLICY.equals(consentType)
                && !CONSENT_TYPE_MARKETING.equals(consentType)) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM,
                    "不支持的同意类型: " + consentType);
        }
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success, String snapshot) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .requestSnapshot(snapshot)
                .build());
    }
}
