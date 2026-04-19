package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.*;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AccountLifecycleAppService 单元测试。
 * <p>
 * 构造真实领域服务实例，仅 mock 底层仓储与适配器，与项目现有测试风格一致。
 */
class AccountLifecycleAppServiceTest {

    // ---- 仓储 mock ----
    private CiamUserRepository userRepository;
    private CiamUserIdentityRepository identityRepository;
    private CiamUserCredentialRepository credentialRepository;
    private CiamUserProfileRepository profileRepository;
    private CiamSessionRepository sessionRepository;
    private CiamRefreshTokenRepository refreshTokenRepository;
    private CiamDeactivationRequestRepository deactivationRequestRepository;
    private AuditLogger auditLogger;

    // ---- 适配器 mock ----
    private SmsAdapter smsAdapter;
    private EmailAdapter emailAdapter;

    // ---- 真实实例 ----
    private InMemoryVerificationCodeStore codeStore;
    private VerificationCodeService verificationCodeService;
    private IdentityDomainService identityDomainService;
    private UserDomainService userDomainService;
    private PasswordChangeAppService passwordChangeAppService;
    private PasswordEncoder passwordEncoder;
    private SecurityEventLogger securityEventLogger;

    // ---- 被测服务 ----
    private AccountLifecycleAppService service;

    private static final String USER_ID = "user-lifecycle-001";
    private static final String ADMIN_ID = "admin-001";

    @BeforeEach
    void setUp() {
        userRepository = mock(CiamUserRepository.class);
        identityRepository = mock(CiamUserIdentityRepository.class);
        credentialRepository = mock(CiamUserCredentialRepository.class);
        profileRepository = mock(CiamUserProfileRepository.class);
        sessionRepository = mock(CiamSessionRepository.class);
        refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
        deactivationRequestRepository = mock(CiamDeactivationRequestRepository.class);
        auditLogger = mock(AuditLogger.class);

        securityEventLogger = new SecurityEventLogger();

        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        when(smsAdapter.sendVerificationCode(anyString(), any(), anyString()))
                .thenReturn(AdapterResult.ok());
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(AdapterResult.ok());

        codeStore = new InMemoryVerificationCodeStore();
        verificationCodeService = new VerificationCodeService(codeStore, smsAdapter, emailAdapter);

        FieldEncryptor fieldEncryptor = new FieldEncryptor("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        identityDomainService = new IdentityDomainService(identityRepository, fieldEncryptor);

        CiamUserProfileRepository userProfileRepo = mock(CiamUserProfileRepository.class);
        userDomainService = new UserDomainService(userRepository, userProfileRepo);

        passwordEncoder = new PasswordEncoder();
        CredentialDomainService credentialDomainService = new CredentialDomainService(
                credentialRepository, passwordEncoder, new PasswordPolicyService());
        passwordChangeAppService = new PasswordChangeAppService(
                credentialDomainService, sessionRepository, refreshTokenRepository, auditLogger);

        when(userRepository.updateByUserId(any())).thenReturn(1);
        when(sessionRepository.invalidateAllByUserId(anyString())).thenReturn(2);
        when(refreshTokenRepository.revokeAllByUserId(anyString())).thenReturn(1);
        when(deactivationRequestRepository.insert(any())).thenReturn(1);
        when(deactivationRequestRepository.updateByDeactivationRequestId(any())).thenReturn(1);

        service = new AccountLifecycleAppService(
                verificationCodeService,
                identityDomainService,
                userDomainService,
                passwordChangeAppService,
                deactivationRequestRepository,
                userRepository,
                identityRepository,
                credentialRepository,
                profileRepository,
                sessionRepository,
                refreshTokenRepository,
                auditLogger,
                securityEventLogger);
    }

    private CiamUserDo stubUser(UserStatus status) {
        CiamUserDo user = new CiamUserDo();
        user.setUserId(USER_ID);
        user.setUserStatus(status.getCode());
        user.setRowValid(1);
        return user;
    }

    private CiamDeactivationRequestDo stubDeactivationRequest(String requestId) {
        CiamDeactivationRequestDo req = new CiamDeactivationRequestDo();
        req.setDeactivationRequestId(requestId);
        req.setUserId(USER_ID);
        req.setCheckStatus(CheckStatus.PENDING.getCode());
        req.setReviewStatus(ReviewStatus.PENDING.getCode());
        req.setExecuteStatus(ExecuteStatus.PENDING.getCode());
        req.setRetainAuditOnly(1);
        req.setRowValid(1);
        return req;
    }

    // ---- 11.1 忘记密码 ----

    @Nested
    class ForgotPasswordTests {

        @Test
        void sendsSmsCodeForMobile() {
            service.forgotPassword(IdentityType.MOBILE, "13800138000", "client-1");
            verify(smsAdapter).sendVerificationCode(eq("13800138000"), isNull(), anyString());
        }

        @Test
        void sendsEmailCodeForEmail() {
            service.forgotPassword(IdentityType.EMAIL, "user@example.com", "client-1");
            verify(emailAdapter).sendVerificationCode(eq("user@example.com"), anyString());
        }

        @Test
        void throwsForUnsupportedIdentityType() {
            assertThrows(BusinessException.class,
                    () -> service.forgotPassword(IdentityType.WECHAT, "wx-openid", "client-1"));
        }
    }

    @Nested
    class ResetPasswordWithVerificationTests {

        @Test
        void resetsPasswordAfterCodeVerification() {
            String email = "user@example.com";
            String emailHash = FieldEncryptor.hash(email);
            String clientId = "client-1";

            // Send code first to populate the store
            service.forgotPassword(IdentityType.EMAIL, email, clientId);

            // Retrieve the stored code via the store key convention: vc:email:{hash}:{clientId}
            String userKey = FieldEncryptor.hash(email);
            String codeKey = "vc:email:" + userKey + ":" + clientId;
            String storedCode = codeStore.getCode(codeKey).orElseThrow();

            // Stub identity lookup
            CiamUserIdentityDo identity = new CiamUserIdentityDo();
            identity.setUserId(USER_ID);
            identity.setIdentityType(IdentityType.EMAIL.getCode());
            identity.setIdentityHash(emailHash);
            identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
            when(identityRepository.findByTypeAndHash(IdentityType.EMAIL.getCode(), emailHash))
                    .thenReturn(Optional.of(identity));

            // Stub credential for password reset
            CiamUserCredentialDo cred = new CiamUserCredentialDo();
            cred.setCredentialId("cred-001");
            cred.setUserId(USER_ID);
            cred.setCredentialType(CredentialType.EMAIL_PASSWORD.getCode());
            cred.setCredentialHash(passwordEncoder.encode("OldPass1!"));
            cred.setHashAlgorithm(PasswordEncoder.ALGORITHM);
            cred.setFailCount(0);
            cred.setCredentialStatus(CredentialStatus.VALID.getCode());
            cred.setRowValid(1);
            when(credentialRepository.findByUserIdAndType(USER_ID, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred));
            when(credentialRepository.updateByCredentialId(any())).thenReturn(1);

            service.resetPasswordWithVerification(IdentityType.EMAIL, email, storedCode, "NewPass2@", clientId);

            verify(credentialRepository).updateByCredentialId(any());
            verify(sessionRepository).invalidateAllByUserId(USER_ID);
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
        }

        @Test
        void throwsWhenIdentityNotFound() {
            String email = "unknown@example.com";
            String emailHash = FieldEncryptor.hash(email);
            String clientId = "client-1";

            // Send code first
            service.forgotPassword(IdentityType.EMAIL, email, clientId);
            String userKey = FieldEncryptor.hash(email);
            String codeKey = "vc:email:" + userKey + ":" + clientId;
            String storedCode = codeStore.getCode(codeKey).orElseThrow();

            when(identityRepository.findByTypeAndHash(IdentityType.EMAIL.getCode(), emailHash))
                    .thenReturn(Optional.empty());

            assertThrows(BusinessException.class,
                    () -> service.resetPasswordWithVerification(IdentityType.EMAIL, email, storedCode, "NewPass2@", clientId));
        }
    }

    // ---- 11.2 后台状态管理 ----

    @Nested
    class AdminAccountManagementTests {

        @Test
        void locksActiveAccount() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            service.adminLockAccount(USER_ID, ADMIN_ID);

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.LOCKED.getCode(), captor.getValue().getUserStatus());

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("ACCOUNT", auditCaptor.getValue().getEventType());
            assertEquals("账号锁定", auditCaptor.getValue().getEventName());
            assertTrue(auditCaptor.getValue().getRequestSnapshot().contains(ADMIN_ID));
        }

        @Test
        void lockWithSessionInvalidation() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            service.adminLockAccount(USER_ID, ADMIN_ID, true);

            verify(userRepository).updateByUserId(any());
            verify(sessionRepository).invalidateAllByUserId(USER_ID);
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            verify(auditLogger).log(any(AuditEvent.class));
        }

        @Test
        void lockWithoutSessionInvalidation() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            service.adminLockAccount(USER_ID, ADMIN_ID, false);

            verify(userRepository).updateByUserId(any());
            verify(sessionRepository, never()).invalidateAllByUserId(anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
        }

        @Test
        void unlocksLockedAccount() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.LOCKED)));
            service.adminUnlockAccount(USER_ID, ADMIN_ID);

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.ACTIVE.getCode(), captor.getValue().getUserStatus());

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("ACCOUNT", auditCaptor.getValue().getEventType());
            assertEquals("账号解锁", auditCaptor.getValue().getEventName());
        }

        @Test
        void disablesActiveAccount() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            service.adminDisableAccount(USER_ID, ADMIN_ID);

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.DISABLED.getCode(), captor.getValue().getUserStatus());

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("ACCOUNT", auditCaptor.getValue().getEventType());
            assertEquals("账号禁用", auditCaptor.getValue().getEventName());
        }

        @Test
        void disableWithSessionInvalidation() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            service.adminDisableAccount(USER_ID, ADMIN_ID, true);

            verify(userRepository).updateByUserId(any());
            verify(sessionRepository).invalidateAllByUserId(USER_ID);
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            verify(auditLogger).log(any(AuditEvent.class));
        }

        @Test
        void disableWithoutSessionInvalidation() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            service.adminDisableAccount(USER_ID, ADMIN_ID, false);

            verify(userRepository).updateByUserId(any());
            verify(sessionRepository, never()).invalidateAllByUserId(anyString());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyString());
        }

        @Test
        void enablesDisabledAccount() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.DISABLED)));
            service.adminEnableAccount(USER_ID, ADMIN_ID);

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.ACTIVE.getCode(), captor.getValue().getUserStatus());

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("ACCOUNT", auditCaptor.getValue().getEventType());
            assertEquals("账号启用", auditCaptor.getValue().getEventName());
        }

        @Test
        void throwsOnIllegalTransition() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.LOCKED)));
            assertThrows(BusinessException.class, () -> service.adminDisableAccount(USER_ID, ADMIN_ID));
        }

        @Test
        void throwsWhenLockingAlreadyLockedAccount() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.LOCKED)));
            assertThrows(BusinessException.class, () -> service.adminLockAccount(USER_ID, ADMIN_ID));
        }

        @Test
        void throwsWhenUnlockingActiveAccount() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            assertThrows(BusinessException.class, () -> service.adminUnlockAccount(USER_ID, ADMIN_ID));
        }

        @Test
        void throwsWhenEnablingActiveAccount() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            assertThrows(BusinessException.class, () -> service.adminEnableAccount(USER_ID, ADMIN_ID));
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());
            assertThrows(BusinessException.class, () -> service.adminLockAccount("nonexistent", ADMIN_ID));
        }

        @Test
        void auditContainsOperatorInfo() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            service.adminDisableAccount(USER_ID, ADMIN_ID);

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            AuditEvent event = auditCaptor.getValue();
            assertEquals(USER_ID, event.getUserId());
            assertTrue(event.isSuccess());
            assertNotNull(event.getEventTime());
            assertTrue(event.getRequestSnapshot().contains(ADMIN_ID));
        }
    }

    // ---- 11.3 注销申请与审核 ----

    @Nested
    class DeactivationRequestTests {

        @Test
        void submitsDeactivationRequest() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));

            String requestId = service.submitDeactivationRequest(USER_ID, "app", "不再使用");

            assertNotNull(requestId);
            ArgumentCaptor<CiamDeactivationRequestDo> captor = ArgumentCaptor.forClass(CiamDeactivationRequestDo.class);
            verify(deactivationRequestRepository).insert(captor.capture());
            CiamDeactivationRequestDo saved = captor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals(CheckStatus.PENDING.getCode(), saved.getCheckStatus());
            assertEquals(ReviewStatus.PENDING.getCode(), saved.getReviewStatus());
            assertEquals(ExecuteStatus.PENDING.getCode(), saved.getExecuteStatus());

            ArgumentCaptor<CiamUserDo> userCaptor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(userCaptor.capture());
            assertEquals(UserStatus.DEACTIVATING.getCode(), userCaptor.getValue().getUserStatus());
        }

        @Test
        void approvesDeactivationRequest() {
            String requestId = "req-001";
            when(deactivationRequestRepository.findByDeactivationRequestId(requestId))
                    .thenReturn(Optional.of(stubDeactivationRequest(requestId)));

            service.approveDeactivation(requestId, "reviewer-001");

            ArgumentCaptor<CiamDeactivationRequestDo> captor = ArgumentCaptor.forClass(CiamDeactivationRequestDo.class);
            verify(deactivationRequestRepository).updateByDeactivationRequestId(captor.capture());
            assertEquals(ReviewStatus.APPROVED.getCode(), captor.getValue().getReviewStatus());
            assertEquals("reviewer-001", captor.getValue().getReviewer());
        }
    }

    // ---- 11.4 注销前外部业务校验 ----

    @Nested
    class DeactivationPrerequisitesTests {

        @Test
        void checksPrerequisitesAndPasses() {
            String requestId = "req-002";
            when(deactivationRequestRepository.findByDeactivationRequestId(requestId))
                    .thenReturn(Optional.of(stubDeactivationRequest(requestId)));

            service.checkDeactivationPrerequisites(requestId);

            ArgumentCaptor<CiamDeactivationRequestDo> captor = ArgumentCaptor.forClass(CiamDeactivationRequestDo.class);
            verify(deactivationRequestRepository).updateByDeactivationRequestId(captor.capture());
            assertEquals(CheckStatus.PASSED.getCode(), captor.getValue().getCheckStatus());
        }
    }

    // ---- 11.5 注销执行 ----

    @Nested
    class ExecuteDeactivationTests {

        @Test
        void executesDeactivationAndDeletesUserData() {
            String requestId = "req-003";
            when(deactivationRequestRepository.findByDeactivationRequestId(requestId))
                    .thenReturn(Optional.of(stubDeactivationRequest(requestId)));

            service.executeDeactivation(requestId);

            verify(sessionRepository).invalidateAllByUserId(USER_ID);
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            verify(identityRepository).physicalDeleteByUserId(USER_ID);
            verify(credentialRepository).physicalDeleteByUserId(USER_ID);
            verify(profileRepository).physicalDeleteByUserId(USER_ID);
            verify(userRepository).physicalDeleteByUserId(USER_ID);

            ArgumentCaptor<CiamDeactivationRequestDo> captor = ArgumentCaptor.forClass(CiamDeactivationRequestDo.class);
            verify(deactivationRequestRepository).updateByDeactivationRequestId(captor.capture());
            assertEquals(ExecuteStatus.EXECUTED.getCode(), captor.getValue().getExecuteStatus());
            assertNotNull(captor.getValue().getExecuteTime());
            assertEquals(1, captor.getValue().getRetainAuditOnly());

            verify(auditLogger, atLeastOnce()).log(any(AuditEvent.class));
        }

        @Test
        void throwsWhenRequestNotFound() {
            when(deactivationRequestRepository.findByDeactivationRequestId("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> service.executeDeactivation("nonexistent"));
        }
    }
}
