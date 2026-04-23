package net.hwyz.iov.cloud.sec.ciam.service.application;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.SessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserCredential;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.CredentialDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.PasswordPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PasswordChangeAppService 单元测试。
 * <p>
 * 构造真实的 CredentialDomainService 实例，仅 mock 底层仓储接口，
 * 与项目现有测试风格保持一致。
 */
class PasswordChangeAppServiceTest {

    private UserCredentialRepository credentialRepository;
    private SessionRepository sessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AuditLogger auditLogger;
    private PasswordEncoder passwordEncoder;

    private CredentialDomainService credentialDomainService;
    private PasswordChangeAppService service;

    private static final String USER_ID = "user-001";
    private static final String OLD_PASSWORD = "OldPass1!";
    private static final String NEW_PASSWORD = "NewPass2@";

    @BeforeEach
    void setUp() {
        credentialRepository = mock(UserCredentialRepository.class);
        sessionRepository = mock(SessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        auditLogger = mock(AuditLogger.class);
        passwordEncoder = new PasswordEncoder();

        when(credentialRepository.updateByCredentialId(any())).thenReturn(1);
        when(sessionRepository.invalidateAllByUserId(anyString())).thenReturn(3);
        when(refreshTokenRepository.revokeAllByUserId(anyString())).thenReturn(2);

        credentialDomainService = new CredentialDomainService(
                credentialRepository, passwordEncoder, new PasswordPolicyService());

        service = new PasswordChangeAppService(
                credentialDomainService, sessionRepository, refreshTokenRepository, auditLogger);
    }

    private UserCredential stubCredential(String rawPassword) {
        return UserCredential.builder()
                .credentialId("cred-001")
                .userId(USER_ID)
                .credentialType(CredentialType.EMAIL_PASSWORD.getCode())
                .credentialHash(passwordEncoder.encode(rawPassword))
                .hashAlgorithm(PasswordEncoder.ALGORITHM)
                .failCount(0)
                .credentialStatus(CredentialStatus.VALID.getCode())
                .build();
    }

    @Nested
    class ChangePasswordAndInvalidateSessionsTests {

        @Test
        void changesPasswordAndInvalidatesAllSessionsAndTokens() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential(OLD_PASSWORD)));

            service.changePasswordAndInvalidateSessions(USER_ID, OLD_PASSWORD, NEW_PASSWORD);

            // Verify password was updated
            verify(credentialRepository).updateByCredentialId(any(UserCredential.class));
            // Verify all sessions invalidated
            verify(sessionRepository).invalidateAllByUserId(USER_ID);
            // Verify all refresh tokens revoked
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            // Verify audit log
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals("PASSWORD", event.getEventType());
            assertEquals("密码修改", event.getEventName());
            assertTrue(event.isSuccess());
            assertEquals(USER_ID, event.getUserId());
        }

        @Test
        void throwsWhenOldPasswordIncorrect() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential(OLD_PASSWORD)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.changePasswordAndInvalidateSessions(USER_ID, "WrongOld1!", NEW_PASSWORD));

            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
            // Sessions and tokens should NOT be invalidated on failure
            verify(sessionRepository, never()).invalidateAllByUserId(anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
            verify(auditLogger, never()).log(any());
        }

        @Test
        void throwsWhenCredentialNotFound() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.changePasswordAndInvalidateSessions(USER_ID, OLD_PASSWORD, NEW_PASSWORD));

            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
            verify(sessionRepository, never()).invalidateAllByUserId(anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
        }
    }

    @Nested
    class ResetPasswordAndInvalidateSessionsTests {

        @Test
        void resetsPasswordAndInvalidatesAllSessionsAndTokens() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential(OLD_PASSWORD)));

            service.resetPasswordAndInvalidateSessions(USER_ID, NEW_PASSWORD);

            // Verify password was updated
            verify(credentialRepository).updateByCredentialId(any(UserCredential.class));
            // Verify all sessions invalidated
            verify(sessionRepository).invalidateAllByUserId(USER_ID);
            // Verify all refresh tokens revoked
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            // Verify audit log
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals("PASSWORD", event.getEventType());
            assertEquals("密码找回", event.getEventName());
            assertTrue(event.isSuccess());
            assertEquals(USER_ID, event.getUserId());
        }

        @Test
        void throwsWhenCredentialNotFoundOnReset() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resetPasswordAndInvalidateSessions(USER_ID, NEW_PASSWORD));

            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
            verify(sessionRepository, never()).invalidateAllByUserId(anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
        }
    }
}
