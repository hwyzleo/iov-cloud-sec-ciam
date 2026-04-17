package net.hwyz.iov.cloud.sec.ciam.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto.DeviceInfo;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AppleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.CaptchaAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.CaptchaChallenge;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.GoogleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.LocalMobileAuthAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.ThirdPartyUserInfo;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.WechatLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.CredentialStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.CredentialType;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserCredentialDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthenticationAppService 单元测试。
 * <p>
 * 构造真实的领域服务实例（VerificationCodeService、IdentityDomainService、UserDomainService），
 * 仅 mock 底层仓储接口和外部适配器接口，与项目现有测试风格保持一致。
 */
class AuthenticationAppServiceTest {

    // 底层 mock
    private InMemoryVerificationCodeStore codeStore;
    private SmsAdapter smsAdapter;
    private EmailAdapter emailAdapter;
    private CiamUserIdentityRepository identityRepository;
    private CiamUserRepository userRepository;
    private CiamUserProfileRepository userProfileRepository;
    private CiamUserCredentialRepository credentialRepository;
    private CaptchaAdapter captchaAdapter;
    private AuditLogger auditLogger;
    private CiamSessionRepository sessionRepository;
    private CiamRefreshTokenRepository refreshTokenRepository;
    private CiamDeviceRepository deviceRepository;
    private WechatLoginAdapter wechatLoginAdapter;
    private AppleLoginAdapter appleLoginAdapter;
    private GoogleLoginAdapter googleLoginAdapter;
    private LocalMobileAuthAdapter localMobileAuthAdapter;

    // 真实领域服务
    private VerificationCodeService verificationCodeService;
    private IdentityDomainService identityDomainService;
    private UserDomainService userDomainService;
    private CredentialDomainService credentialDomainService;
    private CaptchaDomainService captchaDomainService;
    private SessionDomainService sessionDomainService;
    private DeviceDomainService deviceDomainService;
    private PasswordEncoder passwordEncoder;

    // 被测对象
    private AuthenticationAppService service;

    private static final String AES_KEY_BASE64 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    private static final String MOBILE = "13800138000";
    private static final String COUNTRY_CODE = "+86";
    private static final String CLIENT_ID = "app-client";
    private static final DeviceInfo DEVICE_INFO = DeviceInfo.builder()
            .deviceId("dev-001")
            .deviceName("iPhone 15")
            .deviceOs("iOS 17.0")
            .build();
    private static final String EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "Abcd1234!";

    @BeforeEach
    void setUp() {
        // mock 接口
        codeStore = new InMemoryVerificationCodeStore();
        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        identityRepository = mock(CiamUserIdentityRepository.class);
        userRepository = mock(CiamUserRepository.class);
        userProfileRepository = mock(CiamUserProfileRepository.class);
        credentialRepository = mock(CiamUserCredentialRepository.class);
        captchaAdapter = mock(CaptchaAdapter.class);
        auditLogger = mock(AuditLogger.class);
        sessionRepository = mock(CiamSessionRepository.class);
        refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
        deviceRepository = mock(CiamDeviceRepository.class);
        wechatLoginAdapter = mock(WechatLoginAdapter.class);
        appleLoginAdapter = mock(AppleLoginAdapter.class);
        googleLoginAdapter = mock(GoogleLoginAdapter.class);
        localMobileAuthAdapter = mock(LocalMobileAuthAdapter.class);

        // 默认 SMS 发送成功
        when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                .thenReturn(AdapterResult.ok());

        // 默认 Email 发送成功
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(AdapterResult.ok());

        // 默认仓储 insert 成功
        when(identityRepository.insert(any())).thenReturn(1);
        when(identityRepository.updateByIdentityId(any())).thenReturn(1);
        when(userRepository.insert(any())).thenReturn(1);
        when(userProfileRepository.insert(any())).thenReturn(1);
        when(credentialRepository.insert(any())).thenReturn(1);
        when(credentialRepository.updateByCredentialId(any())).thenReturn(1);

        // 默认 captcha 生成
        when(captchaAdapter.generateChallenge(anyString()))
                .thenReturn(CaptchaChallenge.builder()
                        .challengeId("captcha-001")
                        .challengeType(CaptchaChallenge.CaptchaType.IMAGE)
                        .challengeData("base64data")
                        .build());
        when(captchaAdapter.verifyChallenge(anyString(), anyString())).thenReturn(true);

        // 构造真实领域服务
        FieldEncryptor fieldEncryptor = new FieldEncryptor(AES_KEY_BASE64);
        passwordEncoder = new PasswordEncoder();
        verificationCodeService = new VerificationCodeService(codeStore, smsAdapter, emailAdapter);
        identityDomainService = new IdentityDomainService(identityRepository, fieldEncryptor);
        userDomainService = new UserDomainService(userRepository, userProfileRepository);
        credentialDomainService = new CredentialDomainService(
                credentialRepository, passwordEncoder, new PasswordPolicyService());
        captchaDomainService = new CaptchaDomainService(captchaAdapter, codeStore);
        sessionDomainService = new SessionDomainService(sessionRepository, refreshTokenRepository, deviceRepository);
        deviceDomainService = new DeviceDomainService(deviceRepository);
        JwtTokenService jwtTokenService = new JwtTokenService();

        service = new AuthenticationAppService(
                verificationCodeService, identityDomainService,
                userDomainService, userRepository, auditLogger,
                credentialDomainService, captchaDomainService,
                sessionDomainService,
                wechatLoginAdapter, appleLoginAdapter, googleLoginAdapter,
                localMobileAuthAdapter, jwtTokenService, deviceDomainService);
    }

    /**
     * 向 InMemoryVerificationCodeStore 预置一个有效验证码，模拟"已发送"状态。
     */
    private String seedVerificationCode() {
        String userKey = FieldEncryptor.hash(MOBILE);
        String code = "888888";
        String codeKey = "vc:sms:" + userKey + ":" + CLIENT_ID;
        codeStore.saveCode(codeKey, code, 300);
        return code;
    }

    // ---- sendMobileVerificationCode ----

    @Nested
    class SendMobileVerificationCodeTests {

        @Test
        void delegatesToSmsAdapter() {
            service.sendMobileVerificationCode(MOBILE, COUNTRY_CODE, CLIENT_ID);

            verify(smsAdapter).sendVerificationCode(eq(MOBILE), eq(COUNTRY_CODE), anyString());
        }
    }

    // ---- 新用户注册流程 ----

    @Nested
    class NewUserRegistrationTests {

        @Test
        void registersNewUserWhenIdentityNotFound() {
            String code = seedVerificationCode();
            // 使用 ArgumentCaptor 捕获 bindIdentity 插入的记录，供 markVerified 查询使用
            ArgumentCaptor<CiamUserIdentityDo> identityCaptor = ArgumentCaptor.forClass(CiamUserIdentityDo.class);
            // 调用顺序：
            // 1. findByTypeAndValue → findByTypeAndHash → empty
            // 2. bindIdentity → findByTypeAndHash → empty
            // 3. markVerified → findByTypeAndHash → 返回刚插入的记录
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.empty())   // findByTypeAndValue
                    .thenReturn(Optional.empty())   // bindIdentity 冲突检查
                    .thenAnswer(inv -> {            // markVerified 查询
                        // 返回之前 insert 捕获的记录
                        return Optional.of(identityCaptor.getValue());
                    });
            when(identityRepository.insert(identityCaptor.capture())).thenReturn(1);
            // activate 需要查到用户
            when(userRepository.findByUserId(anyString())).thenAnswer(inv -> {
                String uid = inv.getArgument(0);
                CiamUserDo user = new CiamUserDo();
                user.setUserId(uid);
                user.setUserStatus(UserStatus.PENDING.getCode());
                return Optional.of(user);
            });
            when(userRepository.updateByUserId(any())).thenReturn(1);

            LoginResult result = service.loginByMobileCode(MOBILE, COUNTRY_CODE, code, CLIENT_ID, DEVICE_INFO);

            assertTrue(result.isNewUser());
            assertNotNull(result.getUserId());
            assertNull(result.getSessionId());

            // 验证用户创建
            verify(userRepository).insert(any(CiamUserDo.class));
            // 验证标识绑定
            verify(identityRepository).insert(any(CiamUserIdentityDo.class));
            // 验证标识标记已验证
            verify(identityRepository).updateByIdentityId(any(CiamUserIdentityDo.class));
            // 验证用户激活
            verify(userRepository).updateByUserId(any(CiamUserDo.class));

            // 验证审计日志 - REGISTER_SUCCESS
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            AuditEvent event = captor.getValue();
            assertEquals("REGISTER", event.getEventType());
            assertTrue(event.isSuccess());
        }
    }

    // ---- 已有用户登录流程 ----

    @Nested
    class ExistingUserLoginTests {

        @Test
        void logsInExistingActiveUser() {
            String code = seedVerificationCode();
            String userId = "existing-user-001";

            // 手机号已绑定
            CiamUserIdentityDo identity = stubBoundIdentity(userId);
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(identity));
            // 用户状态正常
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));

            LoginResult result = service.loginByMobileCode(MOBILE, COUNTRY_CODE, code, CLIENT_ID, DEVICE_INFO);

            assertFalse(result.isNewUser());
            assertEquals(userId, result.getUserId());

            // 不应创建新用户
            verify(userRepository, never()).insert(any());

            // 验证审计日志 - LOGIN_SUCCESS
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("LOGIN", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }
    }

    // ---- 验证码无效 ----

    @Nested
    class InvalidVerificationCodeTests {

        @Test
        void throwsWhenVerificationCodeInvalid() {
            // 不预置验证码，直接调用登录
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(MOBILE, COUNTRY_CODE, "wrong-code", CLIENT_ID, DEVICE_INFO));

            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());

            // 验证失败审计日志
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertFalse(captor.getValue().isSuccess());
        }
    }

    // ---- 锁定/禁用账号 ----

    @Nested
    class LockedOrDisabledAccountTests {

        @Test
        void throwsWhenAccountLocked() {
            String code = seedVerificationCode();
            String userId = "locked-user";

            CiamUserIdentityDo identity = stubBoundIdentity(userId);
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(identity));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.LOCKED)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(MOBILE, COUNTRY_CODE, code, CLIENT_ID, DEVICE_INFO));

            assertEquals(CiamErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());

            // 验证失败审计日志
            verify(auditLogger, atLeastOnce()).log(any(AuditEvent.class));
        }

        @Test
        void throwsWhenAccountDisabled() {
            String code = seedVerificationCode();
            String userId = "disabled-user";

            CiamUserIdentityDo identity = stubBoundIdentity(userId);
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(identity));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.DISABLED)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(MOBILE, COUNTRY_CODE, code, CLIENT_ID, DEVICE_INFO));

            assertEquals(CiamErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        }
    }

    // ---- 辅助方法 ----

    private CiamUserIdentityDo stubBoundIdentity(String userId) {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("identity-001");
        identity.setUserId(userId);
        identity.setIdentityType(IdentityType.MOBILE.getCode());
        identity.setIdentityHash(FieldEncryptor.hash(MOBILE));
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        identity.setVerifiedFlag(1);
        identity.setRowValid(1);
        return identity;
    }

    private CiamUserDo stubUser(String userId, UserStatus status) {
        CiamUserDo user = new CiamUserDo();
        user.setUserId(userId);
        user.setUserStatus(status.getCode());
        return user;
    }

    // ---- 邮箱密码登录 ----

    @Nested
    class EmailPasswordLoginTests {

        private CiamUserIdentityDo stubEmailIdentity(String userId) {
            CiamUserIdentityDo identity = new CiamUserIdentityDo();
            identity.setIdentityId("email-identity-001");
            identity.setUserId(userId);
            identity.setIdentityType(IdentityType.EMAIL.getCode());
            identity.setIdentityHash(FieldEncryptor.hash(EMAIL));
            identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
            identity.setVerifiedFlag(1);
            identity.setRowValid(1);
            return identity;
        }

        private CiamUserCredentialDo stubCredential(String userId, String rawPassword) {
            CiamUserCredentialDo cred = new CiamUserCredentialDo();
            cred.setCredentialId("cred-001");
            cred.setUserId(userId);
            cred.setCredentialType(CredentialType.EMAIL_PASSWORD.getCode());
            cred.setCredentialHash(passwordEncoder.encode(rawPassword));
            cred.setHashAlgorithm(PasswordEncoder.ALGORITHM);
            cred.setFailCount(0);
            cred.setCredentialStatus(CredentialStatus.VALID.getCode());
            cred.setRowValid(1);
            return cred;
        }

        @Test
        void loginSuccessWithCorrectPassword() {
            String userId = "email-user-001";
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(stubEmailIdentity(userId)));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));
            when(credentialRepository.findByUserIdAndType(userId, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential(userId, VALID_PASSWORD)));

            LoginResult result = service.loginByEmailPassword(EMAIL, VALID_PASSWORD, CLIENT_ID, null, null);

            assertFalse(result.isNewUser());
            assertEquals(userId, result.getUserId());
            assertFalse(result.isChallengeRequired());
            assertNull(result.getCaptchaChallenge());

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger, atLeastOnce()).log(captor.capture());
            assertTrue(captor.getAllValues().stream().anyMatch(e -> e.isSuccess() && "LOGIN".equals(e.getEventType())));
        }

        @Test
        void throwsCredentialInvalidWhenPasswordWrong() {
            String userId = "email-user-002";
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(stubEmailIdentity(userId)));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));
            when(credentialRepository.findByUserIdAndType(userId, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential(userId, VALID_PASSWORD)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByEmailPassword(EMAIL, "WrongPass1!", CLIENT_ID, null, null));

            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
        }

        @Test
        void returnsChallengeAfterThreeFailedAttempts() {
            String userId = "email-user-003";
            CiamUserCredentialDo cred = stubCredential(userId, VALID_PASSWORD);
            cred.setFailCount(2); // already 2 failures, next will be 3rd

            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(stubEmailIdentity(userId)));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));
            when(credentialRepository.findByUserIdAndType(userId, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(cred));

            LoginResult result = service.loginByEmailPassword(EMAIL, "WrongPass1!", CLIENT_ID, null, null);

            assertTrue(result.isChallengeRequired());
            assertNotNull(result.getCaptchaChallenge());
            assertEquals("captcha-001", result.getCaptchaChallenge().getChallengeId());
        }

        @Test
        void loginSuccessWithCaptchaVerification() {
            String userId = "email-user-004";
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(stubEmailIdentity(userId)));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));
            when(credentialRepository.findByUserIdAndType(userId, CredentialType.EMAIL_PASSWORD.getCode()))
                    .thenReturn(Optional.of(stubCredential(userId, VALID_PASSWORD)));

            // Pre-seed captcha challenge state
            codeStore.saveCode("captcha:captcha-valid", "PENDING", 300);

            LoginResult result = service.loginByEmailPassword(
                    EMAIL, VALID_PASSWORD, CLIENT_ID, "captcha-valid", "42");

            assertFalse(result.isChallengeRequired());
            assertEquals(userId, result.getUserId());
            verify(captchaAdapter).verifyChallenge("captcha-valid", "42");
        }

        @Test
        void throwsAccountLockedWhenUserLocked() {
            String userId = "email-user-005";
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(stubEmailIdentity(userId)));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.LOCKED)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByEmailPassword(EMAIL, VALID_PASSWORD, CLIENT_ID, null, null));

            assertEquals(CiamErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        }

        @Test
        void throwsCredentialInvalidWhenEmailNotFound() {
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByEmailPassword(EMAIL, VALID_PASSWORD, CLIENT_ID, null, null));

            assertEquals(CiamErrorCode.CREDENTIAL_INVALID, ex.getErrorCode());
        }
    }

    // ---- 邮箱验证码登录 ----

    @Nested
    class EmailCodeLoginTests {

        private String seedEmailVerificationCode() {
            String userKey = FieldEncryptor.hash(EMAIL);
            String code = "666666";
            String codeKey = "vc:email:" + userKey + ":" + CLIENT_ID;
            codeStore.saveCode(codeKey, code, 1800);
            return code;
        }

        private CiamUserIdentityDo stubEmailBoundIdentity(String userId) {
            CiamUserIdentityDo identity = new CiamUserIdentityDo();
            identity.setIdentityId("email-identity-001");
            identity.setUserId(userId);
            identity.setIdentityType(IdentityType.EMAIL.getCode());
            identity.setIdentityHash(FieldEncryptor.hash(EMAIL));
            identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
            identity.setVerifiedFlag(1);
            identity.setRowValid(1);
            return identity;
        }

        @Test
        void logsInExistingUserWithEmailCode() {
            String code = seedEmailVerificationCode();
            String userId = "existing-email-user";

            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(stubEmailBoundIdentity(userId)));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));

            LoginResult result = service.loginByEmailCode(EMAIL, code, CLIENT_ID);

            assertFalse(result.isNewUser());
            assertEquals(userId, result.getUserId());
            verify(userRepository, never()).insert(any());
        }

        @Test
        void registersNewUserWhenEmailNotFound() {
            String code = seedEmailVerificationCode();

            ArgumentCaptor<CiamUserIdentityDo> identityCaptor = ArgumentCaptor.forClass(CiamUserIdentityDo.class);
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.empty())   // findByTypeAndValue
                    .thenReturn(Optional.empty())   // bindIdentity conflict check
                    .thenAnswer(inv -> Optional.of(identityCaptor.getValue())); // markVerified
            when(identityRepository.insert(identityCaptor.capture())).thenReturn(1);
            when(userRepository.findByUserId(anyString())).thenAnswer(inv -> {
                String uid = inv.getArgument(0);
                CiamUserDo user = new CiamUserDo();
                user.setUserId(uid);
                user.setUserStatus(UserStatus.PENDING.getCode());
                return Optional.of(user);
            });
            when(userRepository.updateByUserId(any())).thenReturn(1);

            LoginResult result = service.loginByEmailCode(EMAIL, code, CLIENT_ID);

            assertTrue(result.isNewUser());
            assertNotNull(result.getUserId());
            verify(userRepository).insert(any(CiamUserDo.class));
            verify(identityRepository).insert(any(CiamUserIdentityDo.class));

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("REGISTER", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }
    }

    // ---- 微信登录 ----

    @Nested
    class WechatLoginTests {

        @Test
        void registersNewUserOnFirstWechatLogin() {
            String wechatCode = "wx-auth-code-001";
            ThirdPartyUserInfo userInfo = ThirdPartyUserInfo.builder()
                    .subject("wx-openid-001")
                    .nickname("微信用户")
                    .build();
            when(wechatLoginAdapter.getUserInfo(wechatCode)).thenReturn(userInfo);

            // subject 未绑定
            ArgumentCaptor<CiamUserIdentityDo> identityCaptor = ArgumentCaptor.forClass(CiamUserIdentityDo.class);
            when(identityRepository.findByTypeAndHash(eq("wechat"), anyString()))
                    .thenReturn(Optional.empty())   // findByTypeAndValue
                    .thenReturn(Optional.empty());  // bindIdentity conflict check
            when(identityRepository.insert(identityCaptor.capture())).thenReturn(1);
            // activate 需要查到用户
            when(userRepository.findByUserId(anyString())).thenAnswer(inv -> {
                String uid = inv.getArgument(0);
                CiamUserDo user = new CiamUserDo();
                user.setUserId(uid);
                user.setUserStatus(UserStatus.PENDING.getCode());
                return Optional.of(user);
            });
            when(userRepository.updateByUserId(any())).thenReturn(1);

            LoginResult result = service.loginByWechat(wechatCode, CLIENT_ID);

            assertTrue(result.isNewUser());
            assertNotNull(result.getUserId());
            verify(userRepository).insert(any(CiamUserDo.class));
            verify(identityRepository).insert(any(CiamUserIdentityDo.class));

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("REGISTER", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void logsInExistingUserOnWechatLogin() {
            String wechatCode = "wx-auth-code-002";
            String userId = "existing-wx-user";
            ThirdPartyUserInfo userInfo = ThirdPartyUserInfo.builder()
                    .subject("wx-openid-002")
                    .build();
            when(wechatLoginAdapter.getUserInfo(wechatCode)).thenReturn(userInfo);

            CiamUserIdentityDo identity = new CiamUserIdentityDo();
            identity.setIdentityId("wx-identity-001");
            identity.setUserId(userId);
            identity.setIdentityType(IdentityType.WECHAT.getCode());
            identity.setIdentityHash(FieldEncryptor.hash("wx-openid-002"));
            identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
            identity.setRowValid(1);

            when(identityRepository.findByTypeAndHash(eq("wechat"), anyString()))
                    .thenReturn(Optional.of(identity));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));

            LoginResult result = service.loginByWechat(wechatCode, CLIENT_ID);

            assertFalse(result.isNewUser());
            assertEquals(userId, result.getUserId());
            verify(userRepository, never()).insert(any());
        }
    }

    // ---- Apple 登录 ----

    @Nested
    class AppleLoginTests {

        @Test
        void registersNewUserOnFirstAppleLogin() {
            String identityToken = "apple-token-001";
            ThirdPartyUserInfo userInfo = ThirdPartyUserInfo.builder()
                    .subject("apple-subject-001")
                    .email("user@icloud.com")
                    .build();
            when(appleLoginAdapter.verifyIdentityToken(identityToken)).thenReturn(userInfo);

            // subject 未绑定，email 也未绑定
            when(identityRepository.findByTypeAndHash(eq("apple"), anyString()))
                    .thenReturn(Optional.empty())   // findByTypeAndValue
                    .thenReturn(Optional.empty());  // bindIdentity conflict check
            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.empty());  // checkConflict for email

            ArgumentCaptor<CiamUserIdentityDo> identityCaptor = ArgumentCaptor.forClass(CiamUserIdentityDo.class);
            when(identityRepository.insert(identityCaptor.capture())).thenReturn(1);
            when(userRepository.findByUserId(anyString())).thenAnswer(inv -> {
                String uid = inv.getArgument(0);
                CiamUserDo user = new CiamUserDo();
                user.setUserId(uid);
                user.setUserStatus(UserStatus.PENDING.getCode());
                return Optional.of(user);
            });
            when(userRepository.updateByUserId(any())).thenReturn(1);

            LoginResult result = service.loginByApple(identityToken, CLIENT_ID);

            assertTrue(result.isNewUser());
            assertNotNull(result.getUserId());
            verify(appleLoginAdapter).verifyIdentityToken(identityToken);
        }
    }

    // ---- Google 登录 ----

    @Nested
    class GoogleLoginTests {

        @Test
        void logsInExistingUserOnGoogleLogin() {
            String idToken = "google-token-001";
            String userId = "existing-google-user";
            ThirdPartyUserInfo userInfo = ThirdPartyUserInfo.builder()
                    .subject("google-subject-001")
                    .email("user@gmail.com")
                    .build();
            when(googleLoginAdapter.verifyIdToken(idToken)).thenReturn(userInfo);

            CiamUserIdentityDo identity = new CiamUserIdentityDo();
            identity.setIdentityId("google-identity-001");
            identity.setUserId(userId);
            identity.setIdentityType(IdentityType.GOOGLE.getCode());
            identity.setIdentityHash(FieldEncryptor.hash("google-subject-001"));
            identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
            identity.setRowValid(1);

            when(identityRepository.findByTypeAndHash(eq("google"), anyString()))
                    .thenReturn(Optional.of(identity));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));

            LoginResult result = service.loginByGoogle(idToken, CLIENT_ID);

            assertFalse(result.isNewUser());
            assertEquals(userId, result.getUserId());
            verify(googleLoginAdapter).verifyIdToken(idToken);
            verify(userRepository, never()).insert(any());
        }
    }

    // ---- 第三方登录冲突 ----

    @Nested
    class ThirdPartyConflictTests {

        @Test
        void throwsMergeRequestPendingWhenEmailConflicts() {
            String identityToken = "apple-token-conflict";
            ThirdPartyUserInfo userInfo = ThirdPartyUserInfo.builder()
                    .subject("apple-subject-conflict")
                    .email("conflict@example.com")
                    .build();
            when(appleLoginAdapter.verifyIdentityToken(identityToken)).thenReturn(userInfo);

            // subject 未绑定
            when(identityRepository.findByTypeAndHash(eq("apple"), anyString()))
                    .thenReturn(Optional.empty());

            // email 已绑定到另一个用户 → 冲突
            CiamUserIdentityDo conflictIdentity = new CiamUserIdentityDo();
            conflictIdentity.setIdentityId("email-conflict-001");
            conflictIdentity.setUserId("other-user-999");
            conflictIdentity.setIdentityType(IdentityType.EMAIL.getCode());
            conflictIdentity.setIdentityHash(FieldEncryptor.hash("conflict@example.com"));
            conflictIdentity.setIdentityStatus(IdentityStatus.BOUND.getCode());
            conflictIdentity.setRowValid(1);

            when(identityRepository.findByTypeAndHash(eq("email"), anyString()))
                    .thenReturn(Optional.of(conflictIdentity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByApple(identityToken, CLIENT_ID));

            assertEquals(CiamErrorCode.MERGE_REQUEST_PENDING, ex.getErrorCode());
        }
    }

    // ---- 本机手机号登录 ----

    @Nested
    class LocalMobileLoginTests {

        @Test
        void registersNewUserOnLocalMobileLogin() {
            String token = "carrier-token-001";
            when(localMobileAuthAdapter.verifyToken(token)).thenReturn(MOBILE);

            // 手机号未绑定
            ArgumentCaptor<CiamUserIdentityDo> identityCaptor = ArgumentCaptor.forClass(CiamUserIdentityDo.class);
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.empty())   // findByTypeAndValue
                    .thenReturn(Optional.empty())   // bindIdentity conflict check
                    .thenAnswer(inv -> Optional.of(identityCaptor.getValue())); // markVerified
            when(identityRepository.insert(identityCaptor.capture())).thenReturn(1);
            when(userRepository.findByUserId(anyString())).thenAnswer(inv -> {
                String uid = inv.getArgument(0);
                CiamUserDo user = new CiamUserDo();
                user.setUserId(uid);
                user.setUserStatus(UserStatus.PENDING.getCode());
                return Optional.of(user);
            });
            when(userRepository.updateByUserId(any())).thenReturn(1);

            LoginResult result = service.loginByLocalMobile(token, CLIENT_ID, DEVICE_INFO);

            assertTrue(result.isNewUser());
            assertNotNull(result.getUserId());
            assertFalse(result.isFallbackRequired());
            verify(userRepository).insert(any(CiamUserDo.class));
            verify(identityRepository).insert(any(CiamUserIdentityDo.class));

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("REGISTER", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void logsInExistingUserOnLocalMobileLogin() {
            String token = "carrier-token-002";
            String userId = "existing-local-user";
            when(localMobileAuthAdapter.verifyToken(token)).thenReturn(MOBILE);

            CiamUserIdentityDo identity = stubBoundIdentity(userId);
            when(identityRepository.findByTypeAndHash(eq("mobile"), anyString()))
                    .thenReturn(Optional.of(identity));
            when(userRepository.findByUserId(userId))
                    .thenReturn(Optional.of(stubUser(userId, UserStatus.ACTIVE)));

            LoginResult result = service.loginByLocalMobile(token, CLIENT_ID, DEVICE_INFO);

            assertFalse(result.isNewUser());
            assertEquals(userId, result.getUserId());
            assertFalse(result.isFallbackRequired());
            verify(userRepository, never()).insert(any());

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("LOGIN", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void returnsFallbackRequiredWhenAdapterReturnsNull() {
            String token = "carrier-token-unsupported";
            when(localMobileAuthAdapter.verifyToken(token)).thenReturn(null);

            LoginResult result = service.loginByLocalMobile(token, CLIENT_ID, DEVICE_INFO);

            assertTrue(result.isFallbackRequired());
            assertNull(result.getUserId());
            assertFalse(result.isNewUser());

            // 验证审计日志记录了失败
            verify(auditLogger).log(any(AuditEvent.class));
        }
    }
}
