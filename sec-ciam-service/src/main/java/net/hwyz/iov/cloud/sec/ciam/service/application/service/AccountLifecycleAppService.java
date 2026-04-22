package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CheckStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ExecuteStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ReviewStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.UserDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeType;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.DeactivationRequestPo;
import org.springframework.stereotype.Service;

/**
 * 账号生命周期应用服务 — 编排忘记密码、后台状态管理、注销流程。
 * <p>
 * 职责：
 * <ul>
 *   <li>忘记密码：发送验证码、校验后重置密码</li>
 *   <li>后台管理：锁定、解锁、禁用、启用账号</li>
 *   <li>注销流程：申请、外部校验、审核、执行</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLifecycleAppService {

    private final VerificationCodeService verificationCodeService;
    private final IdentityDomainService identityDomainService;
    private final UserDomainService userDomainService;
    private final PasswordChangeAppService passwordChangeAppService;
    private final CiamDeactivationRequestRepository deactivationRequestRepository;
    private final CiamUserRepository userRepository;
    private final CiamUserIdentityRepository identityRepository;
    private final CiamUserCredentialRepository credentialRepository;
    private final CiamUserProfileRepository profileRepository;
    private final CiamSessionRepository sessionRepository;
    private final CiamRefreshTokenRepository refreshTokenRepository;
    private final AuditLogger auditLogger;
    private final SecurityEventLogger securityEventLogger;

    // ---- 11.1 忘记密码与重置密码 ----

    /**
     * 忘记密码 — 发送验证码到手机号或邮箱。
     *
     * @param identityType  标识类型（MOBILE 或 EMAIL）
     * @param identityValue 手机号或邮箱
     * @param clientId      客户端标识
     */
    public void forgotPassword(IdentityType identityType, String identityValue, String clientId) {
        String userId = FieldEncryptor.hash(identityValue);
        if (identityType == IdentityType.MOBILE) {
            verificationCodeService.sendSmsCode(identityValue, null, userId, clientId);
        } else if (identityType == IdentityType.EMAIL) {
            verificationCodeService.sendEmailCode(identityValue, userId, clientId);
        } else {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "忘记密码仅支持手机号或邮箱");
        }
    }

    /**
     * 验证码校验后重置密码。
     *
     * @param identityType  标识类型（MOBILE 或 EMAIL）
     * @param identityValue 手机号或邮箱
     * @param code          验证码
     * @param newPassword   新密码
     * @param clientId      客户端标识
     */
    public void resetPasswordWithVerification(IdentityType identityType, String identityValue,
                                              String code, String newPassword, String clientId) {
        String userKey = FieldEncryptor.hash(identityValue);
        VerificationCodeType codeType = (identityType == IdentityType.MOBILE)
                ? VerificationCodeType.SMS : VerificationCodeType.EMAIL;
        verificationCodeService.verifyCode(userKey, clientId, codeType, code);

        UserIdentity identity = identityDomainService.findByTypeAndValue(identityType, identityValue)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        passwordChangeAppService.resetPasswordAndInvalidateSessions(identity.getUserId(), newPassword);
    }

    // ---- 11.2 后台启用、禁用、锁定、解锁 ----

    /**
     * 管理员锁定账号。
     * <p>
     * 流程：状态流转（ACTIVE → LOCKED）→ 可选失效活跃会话 → 审计日志 → 安全事件日志。
     *
     * @param userId             用户业务唯一标识
     * @param adminId            管理员标识
     * @param invalidateSessions 是否同时失效该用户所有活跃会话与令牌
     */
    public void adminLockAccount(String userId, String adminId, boolean invalidateSessions) {
        userDomainService.lock(userId);
        if (invalidateSessions) {
            int sessions = sessionRepository.invalidateAllByUserId(userId);
            int tokens = refreshTokenRepository.revokeAllByUserId(userId);
            log.info("锁定账号同时失效会话: userId={}, sessions={}, tokens={}", userId, sessions, tokens);
        }
        logAudit(userId, AuditEventType.ACCOUNT_LOCK, true, adminId);
        securityEventLogger.log("ACCOUNT_LOCK", userId, null, null,
                "管理员锁定账号, adminId=" + adminId);
        log.info("管理员锁定账号: userId={}, adminId={}, invalidateSessions={}", userId, adminId, invalidateSessions);
    }

    /**
     * 管理员锁定账号（默认不失效会话）。
     */
    public void adminLockAccount(String userId, String adminId) {
        adminLockAccount(userId, adminId, false);
    }

    /**
     * 管理员解锁账号。
     * <p>
     * 流程：状态流转（LOCKED → ACTIVE）→ 审计日志 → 安全事件日志。
     *
     * @param userId  用户业务唯一标识
     * @param adminId 管理员标识
     */
    public void adminUnlockAccount(String userId, String adminId) {
        userDomainService.unlock(userId);
        logAudit(userId, AuditEventType.ACCOUNT_UNLOCK, true, adminId);
        securityEventLogger.log("ACCOUNT_UNLOCK", userId, null, null,
                "管理员解锁账号, adminId=" + adminId);
        log.info("管理员解锁账号: userId={}, adminId={}", userId, adminId);
    }

    /**
     * 管理员禁用账号。
     * <p>
     * 流程：状态流转（ACTIVE → DISABLED）→ 可选失效活跃会话 → 审计日志 → 安全事件日志。
     *
     * @param userId             用户业务唯一标识
     * @param adminId            管理员标识
     * @param invalidateSessions 是否同时失效该用户所有活跃会话与令牌
     */
    public void adminDisableAccount(String userId, String adminId, boolean invalidateSessions) {
        userDomainService.disable(userId);
        if (invalidateSessions) {
            int sessions = sessionRepository.invalidateAllByUserId(userId);
            int tokens = refreshTokenRepository.revokeAllByUserId(userId);
            log.info("禁用账号同时失效会话: userId={}, sessions={}, tokens={}", userId, sessions, tokens);
        }
        logAudit(userId, AuditEventType.ACCOUNT_DISABLE, true, adminId);
        securityEventLogger.log("ACCOUNT_DISABLE", userId, null, null,
                "管理员禁用账号, adminId=" + adminId);
        log.info("管理员禁用账号: userId={}, adminId={}, invalidateSessions={}", userId, adminId, invalidateSessions);
    }

    /**
     * 管理员禁用账号（默认不失效会话）。
     */
    public void adminDisableAccount(String userId, String adminId) {
        adminDisableAccount(userId, adminId, false);
    }

    /**
     * 管理员启用账号。
     * <p>
     * 流程：状态流转（DISABLED → ACTIVE）→ 审计日志 → 安全事件日志。
     *
     * @param userId  用户业务唯一标识
     * @param adminId 管理员标识
     */
    public void adminEnableAccount(String userId, String adminId) {
        userDomainService.enable(userId);
        logAudit(userId, AuditEventType.ACCOUNT_ENABLE, true, adminId);
        securityEventLogger.log("ACCOUNT_ENABLE", userId, null, null,
                "管理员启用账号, adminId=" + adminId);
        log.info("管理员启用账号: userId={}, adminId={}", userId, adminId);
    }

    // ---- 11.3 注销申请与审核 ----

    /**
     * 提交注销申请。
     *
     * @param userId        用户业务唯一标识
     * @param requestSource 申请来源
     * @param requestReason 申请原因
     * @return 注销申请业务 ID
     */
    public String submitDeactivationRequest(String userId, String requestSource, String requestReason) {
        userDomainService.startDeactivation(userId);

        DeactivationRequestPo request = new DeactivationRequestPo();
        request.setDeactivationRequestId(UserIdGenerator.generate());
        request.setUserId(userId);
        request.setRequestSource(requestSource);
        request.setRequestReason(requestReason);
        request.setCheckStatus(CheckStatus.PENDING.getCode());
        request.setReviewStatus(ReviewStatus.PENDING.getCode());
        request.setExecuteStatus(ExecuteStatus.PENDING.getCode());
        request.setRequestedTime(DateTimeUtil.getNowInstant());
        request.setRetainAuditOnly(1);
        request.setRowVersion(1);
        request.setRowValid(1);
        request.setCreateTime(DateTimeUtil.getNowInstant());
        request.setModifyTime(DateTimeUtil.getNowInstant());
        deactivationRequestRepository.insert(request);

        logAudit(userId, AuditEventType.DEACTIVATION_APPLY, true, null);
        log.info("注销申请已提交: userId={}, requestId={}", userId, request.getDeactivationRequestId());
        return request.getDeactivationRequestId();
    }

    /**
     * 审核通过注销申请。
     */
    public void approveDeactivation(String deactivationRequestId, String reviewer) {
        DeactivationRequestPo request = findDeactivationRequest(deactivationRequestId);
        request.setReviewStatus(ReviewStatus.APPROVED.getCode());
        request.setReviewer(reviewer);
        request.setReviewTime(DateTimeUtil.getNowInstant());
        request.setModifyTime(DateTimeUtil.getNowInstant());
        deactivationRequestRepository.updateByDeactivationRequestId(request);

        logAudit(request.getUserId(), AuditEventType.DEACTIVATION_REVIEW, true, reviewer);
        log.info("注销申请审核通过: requestId={}, reviewer={}", deactivationRequestId, reviewer);
    }

    /**
     * 驳回注销申请。
     */
    public void rejectDeactivation(String deactivationRequestId, String reviewer) {
        DeactivationRequestPo request = findDeactivationRequest(deactivationRequestId);
        request.setReviewStatus(ReviewStatus.REJECTED.getCode());
        request.setReviewer(reviewer);
        request.setReviewTime(DateTimeUtil.getNowInstant());
        request.setModifyTime(DateTimeUtil.getNowInstant());
        deactivationRequestRepository.updateByDeactivationRequestId(request);

        // 恢复用户状态（从注销处理中恢复为正常）
        userDomainService.cancelDeactivation(request.getUserId());

        logAudit(request.getUserId(), AuditEventType.DEACTIVATION_REVIEW, false, reviewer);
        log.info("注销申请已驳回: requestId={}, reviewer={}", deactivationRequestId, reviewer);
    }

    // ---- 11.4 注销前外部业务校验 ----

    /**
     * 检查注销前置条件（外部业务校验占位）。
     * <p>
     * 当前为占位实现，默认通过。后续对接外部业务系统检查未完结业务。
     */
    public void checkDeactivationPrerequisites(String deactivationRequestId) {
        DeactivationRequestPo request = findDeactivationRequest(deactivationRequestId);

        // TODO: 对接外部业务系统检查未完结业务
        boolean passed = true;

        request.setCheckStatus(passed ? CheckStatus.PASSED.getCode() : CheckStatus.FAILED.getCode());
        request.setModifyTime(DateTimeUtil.getNowInstant());
        deactivationRequestRepository.updateByDeactivationRequestId(request);

        if (!passed) {
            throw new BusinessException(CiamErrorCode.DEACTIVATION_BLOCKED);
        }
        log.info("注销前置校验通过: requestId={}", deactivationRequestId);
    }

    // ---- 11.5 注销执行与最小审计保留 ----

    /**
     * 执行注销 — 物理删除核心身份数据，仅保留脱敏审计凭证。
     */
    public void executeDeactivation(String deactivationRequestId) {
        DeactivationRequestPo request = findDeactivationRequest(deactivationRequestId);
        String userId = request.getUserId();

        // 失效所有会话与令牌
        sessionRepository.invalidateAllByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId);

        // 物理删除核心身份数据
        identityRepository.physicalDeleteByUserId(userId);
        credentialRepository.physicalDeleteByUserId(userId);
        profileRepository.physicalDeleteByUserId(userId);
        userRepository.physicalDeleteByUserId(userId);

        // 完成注销状态流转（先于物理删除记录状态，此处仅更新注销申请记录）
        request.setExecuteStatus(ExecuteStatus.EXECUTED.getCode());
        request.setExecuteTime(DateTimeUtil.getNowInstant());
        request.setRetainAuditOnly(1);
        request.setModifyTime(DateTimeUtil.getNowInstant());
        deactivationRequestRepository.updateByDeactivationRequestId(request);

        logAudit(userId, AuditEventType.DEACTIVATION_COMPLETE, true, null);
        log.info("注销执行完成: userId={}, requestId={}", userId, deactivationRequestId);
    }

    // ---- 内部方法 ----

    private DeactivationRequestPo findDeactivationRequest(String deactivationRequestId) {
        return deactivationRequestRepository.findByDeactivationRequestId(deactivationRequestId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.INVALID_PARAM, "注销申请不存在"));
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success, String operator) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .requestSnapshot(operator != null ? "operator=" + operator : null)
                .build());
    }
}
