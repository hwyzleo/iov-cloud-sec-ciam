package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.CredentialDomainService;
import org.springframework.stereotype.Service;

/**
 * 密码变更应用服务 — 编排密码修改/重置后的全端会话失效。
 * <p>
 * 职责：
 * <ul>
 *   <li>修改密码（需验证旧密码）后批量失效所有会话与 Refresh Token</li>
 *   <li>重置密码（忘记密码场景）后批量失效所有会话与 Refresh Token</li>
 *   <li>记录审计日志</li>
 * </ul>
 *
 * @see CredentialDomainService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordChangeAppService {

    private final CredentialDomainService credentialDomainService;
    private final CiamSessionRepository sessionRepository;
    private final CiamRefreshTokenRepository refreshTokenRepository;
    private final AuditLogger auditLogger;

    /**
     * 修改密码并使全端会话失效。
     * <p>
     * 流程：
     * <ol>
     *   <li>调用凭据领域服务验证旧密码并更新为新密码</li>
     *   <li>批量失效用户所有有效会话</li>
     *   <li>批量撤销用户所有有效 Refresh Token</li>
     *   <li>记录 PASSWORD_CHANGE 审计事件</li>
     * </ol>
     *
     * @param userId         用户业务唯一标识
     * @param oldRawPassword 旧密码
     * @param newRawPassword 新密码
     */
    public void changePasswordAndInvalidateSessions(String userId,
                                                    String oldRawPassword,
                                                    String newRawPassword) {
        credentialDomainService.changePassword(userId, oldRawPassword, newRawPassword);

        int invalidatedSessions = sessionRepository.invalidateAllByUserId(userId);
        int revokedTokens = refreshTokenRepository.revokeAllByUserId(userId);

        log.info("密码修改后全端会话失效完成: userId={}, invalidatedSessions={}, revokedTokens={}",
                userId, invalidatedSessions, revokedTokens);

        logAudit(userId, AuditEventType.PASSWORD_CHANGE, true);
    }

    /**
     * 重置密码并使全端会话失效。
     * <p>
     * 流程：
     * <ol>
     *   <li>调用凭据领域服务直接重置密码（无需旧密码）</li>
     *   <li>批量失效用户所有有效会话</li>
     *   <li>批量撤销用户所有有效 Refresh Token</li>
     *   <li>记录 PASSWORD_RESET 审计事件</li>
     * </ol>
     *
     * @param userId         用户业务唯一标识
     * @param newRawPassword 新密码
     */
    public void resetPasswordAndInvalidateSessions(String userId, String newRawPassword) {
        credentialDomainService.resetPassword(userId, newRawPassword);

        int invalidatedSessions = sessionRepository.invalidateAllByUserId(userId);
        int revokedTokens = refreshTokenRepository.revokeAllByUserId(userId);

        log.info("密码重置后全端会话失效完成: userId={}, invalidatedSessions={}, revokedTokens={}",
                userId, invalidatedSessions, revokedTokens);

        logAudit(userId, AuditEventType.PASSWORD_RESET, true);
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
