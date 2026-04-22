package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.CredentialDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeType;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserIdentityPo;
import org.springframework.stereotype.Service;

/**
 * 忘记密码与重置密码应用服务 — 编排通过手机号或邮箱完成密码找回的完整流程。
 * <p>
 * 流程：
 * <ol>
 *   <li>用户提交手机号或邮箱，系统查找对应账号并发送验证码</li>
 *   <li>用户提交验证码完成校验</li>
 *   <li>用户设置新密码，系统重置密码并使所有已有会话失效</li>
 * </ol>
 *
 * @see CredentialDomainService
 * @see VerificationCodeService
 * @see IdentityDomainService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetAppService {

    private final IdentityDomainService identityDomainService;
    private final VerificationCodeService verificationCodeService;
    private final CredentialDomainService credentialDomainService;
    private final CiamSessionRepository sessionRepository;
    private final CiamRefreshTokenRepository refreshTokenRepository;
    private final AuditLogger auditLogger;
    private final SecurityEventLogger securityEventLogger;

    /**
     * 步骤一：请求密码重置 — 通过手机号发送验证码。
     * <p>
     * 根据手机号查找已绑定的用户标识，若存在则发送短信验证码。
     * 若手机号未绑定任何账号，抛出 USER_NOT_FOUND。
     *
     * @param mobile      手机号
     * @param countryCode 国家区号
     * @param clientId    客户端标识
     * @return 关联的用户 ID
     */
    public String requestResetByMobile(String mobile, String countryCode, String clientId) {
        UserIdentityPo identity = findBoundIdentity(IdentityType.MOBILE, mobile);
        String userId = identity.getUserId();

        verificationCodeService.sendSmsCode(mobile, countryCode, userId, clientId);

        log.info("密码找回验证码已发送(手机号): userId={}", userId);
        return userId;
    }

    /**
     * 步骤一：请求密码重置 — 通过邮箱发送验证码。
     * <p>
     * 根据邮箱查找已绑定的用户标识，若存在则发送邮箱验证码。
     * 若邮箱未绑定任何账号，抛出 USER_NOT_FOUND。
     *
     * @param email    邮箱地址
     * @param clientId 客户端标识
     * @return 关联的用户 ID
     */
    public String requestResetByEmail(String email, String clientId) {
        UserIdentityPo identity = findBoundIdentity(IdentityType.EMAIL, email);
        String userId = identity.getUserId();

        verificationCodeService.sendEmailCode(email, userId, clientId);

        log.info("密码找回验证码已发送(邮箱): userId={}", userId);
        return userId;
    }

    /**
     * 步骤二：校验验证码。
     *
     * @param userId   用户 ID
     * @param clientId 客户端标识
     * @param type     验证码类型（SMS 或 EMAIL）
     * @param code     用户输入的验证码
     */
    public void verifyResetCode(String userId, String clientId, VerificationCodeType type, String code) {
        verificationCodeService.verifyCode(userId, clientId, type, code);
        log.info("密码找回验证码校验通过: userId={}, type={}", userId, type);
    }

    /**
     * 步骤三：重置密码并使所有已有会话失效。
     * <p>
     * 流程：
     * <ol>
     *   <li>调用凭据领域服务重置密码（含密码策略校验）</li>
     *   <li>批量失效用户所有有效会话</li>
     *   <li>批量撤销用户所有有效 Refresh Token</li>
     *   <li>记录审计日志与安全事件</li>
     * </ol>
     *
     * @param userId         用户 ID
     * @param newRawPassword 新密码
     */
    public void resetPassword(String userId, String newRawPassword) {
        credentialDomainService.resetPassword(userId, newRawPassword);

        int invalidatedSessions = sessionRepository.invalidateAllByUserId(userId);
        int revokedTokens = refreshTokenRepository.revokeAllByUserId(userId);

        log.info("密码重置完成，全端会话已失效: userId={}, invalidatedSessions={}, revokedTokens={}",
                userId, invalidatedSessions, revokedTokens);

        logAudit(userId, AuditEventType.PASSWORD_RESET, true);
        securityEventLogger.log("PASSWORD_RESET", userId, null, null,
                "密码重置成功，已失效会话=" + invalidatedSessions + "，已撤销令牌=" + revokedTokens);
    }

    // ---- 内部方法 ----

    /**
     * 根据标识类型和原值查找已绑定的用户标识，未找到则抛出 USER_NOT_FOUND。
     */
    private UserIdentityPo findBoundIdentity(IdentityType type, String identityValue) {
        return identityDomainService.findByTypeAndValue(type, identityValue)
                .filter(i -> i.getIdentityStatus() == IdentityStatus.BOUND.getCode())
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));
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
