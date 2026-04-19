package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.CredentialDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.PasswordPolicyService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeStore;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeType;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserCredentialDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PasswordResetAppService 单元测试。
 * <p>
 * 构造真实的领域服务实例，仅 mock 底层仓储接口和适配器接口，
 * 与项目现有测试风格保持一致。
 */
class PasswordResetAppServiceTest {

    private CiamUserIdentityRepository identityRepository;
    private CiamUserCredentialRepository credentialRepository;
    private CiamSessionRepository sessionRepository;
    private CiamRefreshTokenRepository refreshTokenRepository;
    private AuditLogger auditLogger;
    private VerificationCodeStore codeStore;
    private SmsAdapter smsAdapter;
    private EmailAdapter emailAdapter;
    private PasswordEncoder passwordEncoder;

    private PasswordResetAppService service;

    private static final String USER_ID = "user-reset-001";
    private static final String MOBILE = "13800138000";
    private static final String COUNTRY_CODE = "+86";
    private static final String EMAIL = "test@example.com";
    private static final String CLIENT_ID = "client-app";
    private static final String NEW_PASSWORD = "NewPass2@";
    private static final String MOBILE_HASH = FieldEncryptor.hash(MOBILE);
    private static final String EMAIL_HASH = FieldEncryptor.hash(EMAIL);

    @BeforeEach
    void setUp() {
        identityRepository = mock(CiamUserIdentityRepository.class);
        credentialRepository = mock(CiamUserCredentialRepository.class);
        sessionRepository = mock(CiamSessionRepository.class);
        refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
        auditLogger = mock(AuditLogger.class);
        codeStore = mock(VerificationCodeStore.class);
        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        passwordEncoder = new PasswordEncoder();

        FieldEncryptor fieldEncryptor = new FieldEncryptor(
                java.util.Base64.getEncoder().encodeToString(new byte[32]));

        IdentityDomainService identityDomainService =
                new IdentityDomainService(identityRepository, fieldEncryptor);

        // Allow rate limit checks to pass
        when(codeStore.setIfAbsent(anyString(), anyInt())).thenReturn(true);
        when(codeStore.incrementDailyCount(anyString(), anyInt())).thenReturn(1L);
        when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                .thenReturn(AdapterResult.ok());
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(AdapterResult.ok());

        VerificationCodeService verificationCodeService =
                new VerificationCodeService(codeStore, smsAdapter, emailAdapter);

        CredentialDomainService credentialDomainService = new CredentialDomainService(
                credentialRepository, passwordEncoder, new PasswordPolicyService());

        when(credentialRepository.updateByCredentialId(any())).thenReturn(1);
        when(sessionRepository.invalidateAllByUserId(anyString())).thenReturn(3);
        when(refreshTokenRepository.revokeAllByUserId(anyString())).thenReturn(2);

        service = new PasswordResetAppService(
                identityDomainService,
                verificationCodeService,
                credentialDomainService,
                sessionRepository,
                refreshTokenRepository,
                auditLogger,
                new SecurityEventLogger());
    }

    private CiamUserIdentityDo stubMobileIdentity() {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("identity-001");
        identity.setUserId(USER_ID);
        identity.setIdentityType(IdentityType.MOBILE.getCode());
        identity.setIdentityHash(MOBILE_HASH);
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        identity.setRowValid(1);
        return identity;
    }

    private CiamUserIdentityDo stubEmailIdentity() {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("identity-002");
        identity.setUserId(USER_ID);
        identity.setIdentityType(IdentityType.EMAIL.getCode());
        identity.setIdentityHash(EMAIL_HASH);
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        identity.setRowValid(1);
        return identity;
    }

    private CiamUserCredentialDo stubCredential() {
        CiamUserCredentialDo cred = new CiamUserCredentialDo();
        cred.setCredentialId("cred-001");
        cred.setUserId(USER_ID);
        cred.setCredentialType(CredentialType.EMAIL_PASSWORD.getCode());
        cred.setCredentialHash(passwordEncoder.encode("OldPass1!"));
        cred.setHashAlgorithm(PasswordEncoder.ALGORITHM);
        cred.setFailCount(0);
        cred.setCredentialStatus(CredentialStatus.VALID.getCode());
        cred.setRowValid(1);
        return cred;
    }

    @Nested
    class RequestResetByMobileTests {

        @Test
        void sendsVerificationCodeAndReturnsUserId() {
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), MOBILE_HASH))
                    .thenReturn(Optional.of(stubMobileIdentity()));

            String result = service.requestResetByMobile(MOBILE, COUNTRY_CODE, CLIENT_ID);

            assertEquals(USER_ID, result);
            verify(smsAdapter).sendVerificationCode(eq(MOBILE), eq(COUNTRY_CODE), anyString());
        }

        @Test
        void throwsWhenMobileNotBound() {
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), MOBILE_HASH))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestResetByMobile(MOBILE, COUNTRY_CODE, CLIENT_ID));

            assertEquals(CiamErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(smsAdapter, never()).sendVerificationCode(anyString(), anyString(), anyString());
        }

        @Test
        void throwsWhenMobileUnbound() {
            CiamUserIdentityDo unbound = stubMobileIdentity();
            unbound.setIdentityStatus(IdentityStatus.UNBOUND.getCode());
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), MOBILE_HASH))
                    .thenReturn(Optional.of(unbound));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestResetByMobile(MOBILE, COUNTRY_CODE, CLIENT_ID));

            assertEquals(CiamErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    class RequestResetByEmailTests {

        @Test
        void sendsVerificationCodeAndReturnsUserId() {
            when(identityRepository.findByTypeAndHash(IdentityType.EMAIL.getCode(), EMAIL_HASH))
                    .thenReturn(Optional.of(stubEmailIdentity()));

            String result = service.requestResetByEmail(EMAIL, CLIENT_ID);

            assertEquals(USER_ID, result);
            verify(emailAdapter).sendVerificationCode(eq(EMAIL), anyString());
        }

        @Test
        void throwsWhenEmailNotBound() {
            when(identityRepository.findByTypeAndHash(IdentityType.EMAIL.getCode(), EMAIL_HASH))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.requestResetByEmail(EMAIL, CLIENT_ID));

            assertEquals(CiamErrorCode.USER_NOT_FOUND, ex.getErrorCode());
            verify(emailAdapter, never()).sendVerificationCode(anyString(), anyString());
        }
    }

    @Nested
    class VerifyResetCodeTests {

        @Test
        void verifiesCodeSuccessfully() {
            when(codeStore.getCode(anyString())).thenReturn(Optional.of("123456"));

            service.verifyResetCode(USER_ID, CLIENT_ID, VerificationCodeType.SMS, "123456");

            verify(codeStore).deleteCode(anyString());
        }

        @Test
        void throwsWhenCodeInvalid() {
            when(codeStore.getCode(anyString())).thenReturn(Optional.of("999999"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyResetCode(USER_ID, CLIENT_ID, VerificationCodeType.EMAIL, "000000"));

            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());
        }

        @Test
        void throwsWhenCodeExpired() {
            when(codeStore.getCode(anyString())).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyResetCode(USER_ID, CLIENT_ID, VerificationCodeType.SMS, "123456"));

            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());
        }
    }

    @Nested
    class ResetPasswordTests {

        @Test
        void resetsPasswordAndInvalidatesAllSessionsAndTokens() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential()));

            service.resetPassword(USER_ID, NEW_PASSWORD);

            verify(credentialRepository).updateByCredentialId(any(CiamUserCredentialDo.class));
            verify(sessionRepository).invalidateAllByUserId(USER_ID);
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals("PASSWORD", event.getEventType());
            assertEquals("密码找回", event.getEventName());
            assertTrue(event.isSuccess());
            assertEquals(USER_ID, event.getUserId());
        }

        @Test
        void throwsWhenCredentialNotFound() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resetPassword(USER_ID, NEW_PASSWORD));

            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
            verify(sessionRepository, never()).invalidateAllByUserId(anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
            verify(auditLogger, never()).log(any());
        }

        @Test
        void throwsWhenNewPasswordTooWeak() {
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.resetPassword(USER_ID, "weak"));

            assertEquals(CiamErrorCode.PASSWORD_COMPLEXITY_INSUFFICIENT, ex.getErrorCode());
            verify(sessionRepository, never()).invalidateAllByUserId(anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
        }
    }
}
