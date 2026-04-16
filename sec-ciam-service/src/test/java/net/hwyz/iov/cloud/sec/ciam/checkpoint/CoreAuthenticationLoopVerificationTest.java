package net.hwyz.iov.cloud.sec.ciam.checkpoint;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.application.AccountLifecycleAppService;
import net.hwyz.iov.cloud.sec.ciam.application.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.application.LoginResult;
import net.hwyz.iov.cloud.sec.ciam.application.PasswordChangeAppService;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.common.audit.SecurityEventLogger;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.MobileAuthController;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.MobileRiskController;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto.*;
import net.hwyz.iov.cloud.sec.ciam.controller.open.OpenOAuthController;
import net.hwyz.iov.cloud.sec.ciam.controller.open.OpenOidcController;
import net.hwyz.iov.cloud.sec.ciam.controller.open.dto.*;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AppleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.CaptchaAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.GoogleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.LocalMobileAuthAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.ThirdPartyUserInfo;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.WechatLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.ChallengeStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeactivationRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamMfaChallengeRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRiskEventRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.service.CaptchaDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.CredentialDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.DeviceAuthorizationResponse;
import net.hwyz.iov.cloud.sec.ciam.domain.service.DeviceAuthorizationService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.MfaDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.OAuthAuthorizationService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.OidcDiscoveryDocument;
import net.hwyz.iov.cloud.sec.ciam.domain.service.OidcService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.OidcUserInfo;
import net.hwyz.iov.cloud.sec.ciam.domain.service.PasswordPolicyService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.RefreshTokenDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.SessionDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.UserDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.VerificationCodeService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeactivationRequestDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamMfaChallengeDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamSessionDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;

/**
 * 检查点 17：核心认证闭环验证。
 * <p>
 * 验证以下 8 条核心链路从 Controller → Application Service → Domain Service 完整贯通：
 * <ol>
 *   <li>手机号登录（短信验证码注册/登录）</li>
 *   <li>邮箱登录（密码登录 + 验证码登录）</li>
 *   <li>第三方登录（微信、Apple、Google）</li>
 *   <li>扫码登录 / Device Authorization Grant</li>
 *   <li>OAuth 2.0 / OIDC（Authorization Code + PKCE、Client Credentials、OIDC endpoints）</li>
 *   <li>会话管理（查询、下线、全端失效）</li>
 *   <li>MFA（挑战触发与校验）</li>
 *   <li>注销流程（申请 → 审核 → 执行）</li>
 * </ol>
 */
@DisplayName("检查点 17：核心认证闭环验证")
class CoreAuthenticationLoopVerificationTest {

    // ---- 共享 mock 仓储 ----
    CiamUserRepository userRepository;
    CiamUserIdentityRepository identityRepository;
    CiamUserCredentialRepository credentialRepository;
    CiamUserProfileRepository profileRepository;
    CiamSessionRepository sessionRepository;
    CiamRefreshTokenRepository refreshTokenRepository;
    CiamDeviceRepository deviceRepository;
    CiamOAuthClientRepository clientRepository;
    CiamAuthCodeRepository authCodeRepository;
    CiamMfaChallengeRepository mfaChallengeRepository;
    CiamDeactivationRequestRepository deactivationRequestRepository;
    CiamRiskEventRepository riskEventRepository;

    // ---- 共享 mock 适配器 ----
    SmsAdapter smsAdapter;
    EmailAdapter emailAdapter;
    WechatLoginAdapter wechatLoginAdapter;
    AppleLoginAdapter appleLoginAdapter;
    GoogleLoginAdapter googleLoginAdapter;
    LocalMobileAuthAdapter localMobileAuthAdapter;
    CaptchaAdapter captchaAdapter;
    AuditLogger auditLogger;
    SecurityEventLogger securityEventLogger;

    // ---- 共享基础设施 ----
    FieldEncryptor fieldEncryptor;
    PasswordEncoder passwordEncoder;
    InMemoryVerificationCodeStore verificationCodeStore;

    // ---- 共享领域服务 ----
    VerificationCodeService vcService;
    IdentityDomainService identityService;
    UserDomainService userService;
    CredentialDomainService credentialService;
    SessionDomainService sessionService;
    PasswordPolicyService passwordPolicyService;
    CaptchaDomainService captchaService;

    @BeforeEach
    void setUp() {
        // 仓储
        userRepository = mock(CiamUserRepository.class);
        identityRepository = mock(CiamUserIdentityRepository.class);
        credentialRepository = mock(CiamUserCredentialRepository.class);
        profileRepository = mock(CiamUserProfileRepository.class);
        sessionRepository = mock(CiamSessionRepository.class);
        refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
        deviceRepository = mock(CiamDeviceRepository.class);
        clientRepository = mock(CiamOAuthClientRepository.class);
        authCodeRepository = mock(CiamAuthCodeRepository.class);
        mfaChallengeRepository = mock(CiamMfaChallengeRepository.class);
        deactivationRequestRepository = mock(CiamDeactivationRequestRepository.class);
        riskEventRepository = mock(CiamRiskEventRepository.class);

        // 适配器
        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        wechatLoginAdapter = mock(WechatLoginAdapter.class);
        appleLoginAdapter = mock(AppleLoginAdapter.class);
        googleLoginAdapter = mock(GoogleLoginAdapter.class);
        localMobileAuthAdapter = mock(LocalMobileAuthAdapter.class);
        captchaAdapter = mock(CaptchaAdapter.class);
        auditLogger = mock(AuditLogger.class);
        securityEventLogger = new SecurityEventLogger();

        // 基础设施
        fieldEncryptor = new FieldEncryptor("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        passwordEncoder = new PasswordEncoder();
        verificationCodeStore = new InMemoryVerificationCodeStore();

        when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                .thenReturn(new AdapterResult(true, "ok"));
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(new AdapterResult(true, "ok"));

        // 领域服务
        vcService = new VerificationCodeService(verificationCodeStore, smsAdapter, emailAdapter);
        identityService = new IdentityDomainService(identityRepository, fieldEncryptor);
        userService = new UserDomainService(userRepository, profileRepository);
        credentialService = new CredentialDomainService(credentialRepository, passwordEncoder, passwordPolicyService);
        sessionService = new SessionDomainService(sessionRepository, refreshTokenRepository, deviceRepository);
        passwordPolicyService = new PasswordPolicyService();
        captchaService = new CaptchaDomainService(captchaAdapter, verificationCodeStore);
    }

    // ---- 辅助方法 ----

    private CiamUserIdentityDo stubIdentity(String userId, String type, String value) {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("id-" + userId);
        identity.setUserId(userId);
        identity.setIdentityType(type);
        identity.setIdentityValue(fieldEncryptor.encrypt(value));
        identity.setIdentityHash(FieldEncryptor.hash(value));
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        identity.setRowValid(1);
        return identity;
    }

    private CiamUserDo stubUser(String userId, int status) {
        CiamUserDo user = new CiamUserDo();
        user.setUserId(userId);
        user.setUserStatus(status);
        user.setRowValid(1);
        return user;
    }

    private CiamOAuthClientDo stubClient(String clientId, String clientType) {
        CiamOAuthClientDo client = new CiamOAuthClientDo();
        client.setClientId(clientId);
        client.setClientName("Test Client");
        client.setClientType(clientType);
        client.setGrantTypes("authorization_code,client_credentials,refresh_token,urn:ietf:params:oauth:grant-type:device_code");
        client.setScopes("openid,profile,email");
        client.setRedirectUris("https://app.example.com/callback");
        client.setPkceRequired(1);
        client.setAccessTokenTtl(1800);
        client.setRefreshTokenTtl(2592000);
        client.setClientStatus(1);
        client.setRowValid(1);
        return client;
    }

    private void stubNewUserActivation() {
        // 用户创建后，activate 需要能找到刚创建的用户（PENDING 状态）
        when(userRepository.findByUserId(anyString())).thenAnswer(inv -> {
            String userId = inv.getArgument(0);
            return Optional.of(stubUser(userId, UserStatus.PENDING.getCode()));
        });

        // 第三方登录标识不存在（首次查找返回 empty）
        // bindIdentity 后 markVerified 需要能找到刚绑定的标识
        // 使用 thenAnswer 模拟：第一次返回 empty，后续返回已绑定的标识
        java.util.Map<String, CiamUserIdentityDo> insertedIdentities = new java.util.concurrent.ConcurrentHashMap<>();
        doAnswer(inv -> {
            CiamUserIdentityDo entity = inv.getArgument(0);
            String key = entity.getIdentityType() + ":" + entity.getIdentityHash();
            insertedIdentities.put(key, entity);
            return 1;
        }).when(identityRepository).insert(any(CiamUserIdentityDo.class));

        when(identityRepository.findByTypeAndHash(anyString(), anyString())).thenAnswer(inv -> {
            String type = inv.getArgument(0);
            String hash = inv.getArgument(1);
            String key = type + ":" + hash;
            CiamUserIdentityDo found = insertedIdentities.get(key);
            return Optional.ofNullable(found);
        });
    }

    /**
     * 构建 MobileAuthController，复用共享领域服务。
     */
    private MobileAuthController buildAuthController() {
        // AuthenticationAppService 字段顺序（@RequiredArgsConstructor）:
        // verificationCodeService, identityDomainService, userDomainService,
        // userRepository, auditLogger, credentialDomainService, captchaDomainService,
        // sessionDomainService, wechatLoginAdapter, appleLoginAdapter,
        // googleLoginAdapter, localMobileAuthAdapter, jwtTokenService
        JwtTokenService jwtTokenService = new JwtTokenService();
        AuthenticationAppService authAppService = new AuthenticationAppService(
                vcService, identityService, userService,
                userRepository, auditLogger, credentialService, captchaService,
                sessionService, wechatLoginAdapter, appleLoginAdapter,
                googleLoginAdapter, localMobileAuthAdapter, jwtTokenService);
        return new MobileAuthController(authAppService, vcService, captchaService);
    }

    // ========================================================================
    // 链路 1：手机号登录
    // ========================================================================
    @Nested
    @DisplayName("链路 1：手机号登录闭环")
    class MobileLoginLoop {

        MobileAuthController authController;

        @BeforeEach
        void init() {
            authController = buildAuthController();
        }

        @Test
        @DisplayName("Controller → AppService → DomainService：手机号验证码登录（新用户自动注册）")
        void mobileLogin_newUser_autoRegister() {
            stubNewUserActivation();

            // 捕获发送的验证码
            AtomicReference<String> capturedCode = new AtomicReference<>();
            when(smsAdapter.sendVerificationCode(eq("13800001111"), eq("+86"), anyString()))
                    .thenAnswer(inv -> {
                        capturedCode.set(inv.getArgument(2));
                        return new AdapterResult(true, "ok");
                    });

            authController.sendMobileCode("app-client", SendMobileCodeRequest.builder()
                    .mobile("13800001111").countryCode("+86").build());
            assertNotNull(capturedCode.get(), "验证码应已发送");

            LoginResult result = authController.loginByMobile("app-client", MobileLoginRequest.builder()
                    .mobile("13800001111").countryCode("+86").code(capturedCode.get())
                    .build()).getData();
            assertNotNull(result.getUserId(), "应返回用户 ID");
            assertTrue(result.isNewUser(), "应标记为新用户");
        }

        @Test
        @DisplayName("Controller → AppService → DomainService：手机号验证码登录（已有用户）")
        void mobileLogin_existingUser() {
            CiamUserIdentityDo identity = stubIdentity("U001", IdentityType.MOBILE.getCode(), "13800001111");
            when(identityRepository.findByTypeAndHash(eq(IdentityType.MOBILE.getCode()), anyString()))
                    .thenReturn(Optional.of(identity));
            when(userRepository.findByUserId("U001"))
                    .thenReturn(Optional.of(stubUser("U001", UserStatus.ACTIVE.getCode())));

            AtomicReference<String> capturedCode = new AtomicReference<>();
            when(smsAdapter.sendVerificationCode(eq("13800001111"), eq("+86"), anyString()))
                    .thenAnswer(inv -> {
                        capturedCode.set(inv.getArgument(2));
                        return new AdapterResult(true, "ok");
                    });

            authController.sendMobileCode("app-client", SendMobileCodeRequest.builder()
                    .mobile("13800001111").countryCode("+86").build());
            assertNotNull(capturedCode.get());

            LoginResult result = authController.loginByMobile("app-client", MobileLoginRequest.builder()
                    .mobile("13800001111").countryCode("+86").code(capturedCode.get())
                    .build()).getData();
            assertEquals("U001", result.getUserId());
            assertFalse(result.isNewUser());
        }
    }

    // ========================================================================
    // 链路 2：邮箱登录
    // ========================================================================
    @Nested
    @DisplayName("链路 2：邮箱登录闭环")
    class EmailLoginLoop {

        MobileAuthController authController;

        @BeforeEach
        void init() {
            authController = buildAuthController();
        }

        @Test
        @DisplayName("Controller → AppService → DomainService：邮箱密码登录")
        void emailPasswordLogin() {
            String rawPassword = "P@ssw0rd!";
            String hashedPassword = passwordEncoder.encode(rawPassword);

            CiamUserIdentityDo identity = stubIdentity("U002", IdentityType.EMAIL.getCode(), "user@example.com");
            when(identityRepository.findByTypeAndHash(eq(IdentityType.EMAIL.getCode()), anyString()))
                    .thenReturn(Optional.of(identity));
            when(userRepository.findByUserId("U002"))
                    .thenReturn(Optional.of(stubUser("U002", UserStatus.ACTIVE.getCode())));
            when(credentialRepository.findByUserIdAndType(eq("U002"), anyString()))
                    .thenReturn(Optional.of(new net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserCredentialDo() {{
                        setCredentialId("cred-001");
                        setUserId("U002");
                        setCredentialHash(hashedPassword);
                        setHashAlgorithm("bcrypt");
                        setFailCount(0);
                        setCredentialStatus(1);
                        setRowValid(1);
                    }}));

            LoginResult result = authController.loginByEmailPassword("app-client", EmailPasswordLoginRequest.builder()
                    .email("user@example.com").password(rawPassword).build()).getData();
            assertEquals("U002", result.getUserId());
            assertFalse(result.isNewUser());
        }

        @Test
        @DisplayName("Controller → AppService → DomainService：邮箱验证码登录（新用户自动注册）")
        void emailCodeLogin_newUser() {
            stubNewUserActivation();

            AtomicReference<String> capturedCode = new AtomicReference<>();
            when(emailAdapter.sendVerificationCode(eq("new@example.com"), anyString()))
                    .thenAnswer(inv -> {
                        capturedCode.set(inv.getArgument(1));
                        return new AdapterResult(true, "ok");
                    });

            authController.sendEmailCode("app-client", SendEmailCodeRequest.builder()
                    .email("new@example.com").build());
            assertNotNull(capturedCode.get());

            LoginResult result = authController.loginByEmailCode("app-client", EmailCodeLoginRequest.builder()
                    .email("new@example.com").code(capturedCode.get()).build()).getData();
            assertNotNull(result.getUserId());
            assertTrue(result.isNewUser());
        }
    }

    // ========================================================================
    // 链路 3：第三方登录
    // ========================================================================
    @Nested
    @DisplayName("链路 3：第三方登录闭环")
    class ThirdPartyLoginLoop {

        MobileAuthController authController;

        @BeforeEach
        void init() {
            authController = buildAuthController();
        }

        @Test
        @DisplayName("Controller → AppService → Adapter：微信登录（新用户自动注册）")
        void wechatLogin_newUser() {
            stubNewUserActivation();
            when(wechatLoginAdapter.getUserInfo("wx-code-123"))
                    .thenReturn(new ThirdPartyUserInfo("wx-openid-001", "wechat", "WxUser", null, null));

            LoginResult result = authController.loginByWechat("app-client", ThirdPartyLoginRequest.builder()
                    .token("wx-code-123").build()).getData();
            assertNotNull(result.getUserId());
            assertTrue(result.isNewUser());
        }

        @Test
        @DisplayName("Controller → AppService → Adapter：Apple 登录（新用户自动注册）")
        void appleLogin_newUser() {
            stubNewUserActivation();
            when(appleLoginAdapter.verifyIdentityToken("apple-token-123"))
                    .thenReturn(new ThirdPartyUserInfo("apple-sub-001", "apple", "AppleUser", null, null));

            LoginResult result = authController.loginByApple("app-client", ThirdPartyLoginRequest.builder()
                    .token("apple-token-123").build()).getData();
            assertNotNull(result.getUserId());
            assertTrue(result.isNewUser());
        }

        @Test
        @DisplayName("Controller → AppService → Adapter：Google 登录（新用户自动注册）")
        void googleLogin_newUser() {
            stubNewUserActivation();
            when(googleLoginAdapter.verifyIdToken("google-token-123"))
                    .thenReturn(new ThirdPartyUserInfo("google-sub-001", "google", "GoogleUser", null, null));

            LoginResult result = authController.loginByGoogle("app-client", ThirdPartyLoginRequest.builder()
                    .token("google-token-123").build()).getData();
            assertNotNull(result.getUserId());
            assertTrue(result.isNewUser());
        }
    }

    // ========================================================================
    // 链路 4：扫码登录 / Device Authorization Grant
    // ========================================================================
    @Nested
    @DisplayName("链路 4：扫码登录 / Device Authorization Grant 闭环")
    class DeviceAuthorizationLoop {

        OpenOAuthController oAuthController;

        @BeforeEach
        void init() {
            DeviceAuthorizationService deviceAuthService = new DeviceAuthorizationService(
                    clientRepository, verificationCodeStore);
            JwtTokenService jwtTokenService = new JwtTokenService();

            OAuthAuthorizationService oAuthAuthService = new OAuthAuthorizationService(
                    authCodeRepository, clientRepository, passwordEncoder);
            RefreshTokenDomainService refreshTokenService = new RefreshTokenDomainService(
                    refreshTokenRepository);

            oAuthController = new OpenOAuthController(oAuthAuthService, deviceAuthService, refreshTokenService, jwtTokenService);
            when(clientRepository.findByClientId("vehicle-client"))
                    .thenReturn(Optional.of(stubClient("vehicle-client", "public")));
        }

        @Test
        @DisplayName("Controller → DomainService：设备授权发起 → 用户确认 → 轮询获取令牌")
        void deviceAuthorizationGrant_fullLoop() {
            // 1. 设备发起授权
            ApiResponse<DeviceAuthorizationResponse> initResp =
                    oAuthController.deviceAuthorize(DeviceAuthorizeRequest.builder()
                            .clientId("vehicle-client").scope("openid").build());
            assertEquals("000000", initResp.getCode());
            DeviceAuthorizationResponse deviceResp = initResp.getData();
            assertNotNull(deviceResp.getDeviceCode());
            assertNotNull(deviceResp.getUserCode());

            // 2. 轮询应返回 pending
            assertThrows(BusinessException.class, () ->
                    oAuthController.token(TokenRequest.builder()
                            .grantType("urn:ietf:params:oauth:grant-type:device_code")
                            .clientId("vehicle-client").deviceCode(deviceResp.getDeviceCode()).build()));

            // 3. 用户确认
            ApiResponse<Void> approveResp = oAuthController.approveDevice(ApproveDeviceRequest.builder()
                    .userCode(deviceResp.getUserCode()).userId("U-VEHICLE-001").build());
            assertEquals("000000", approveResp.getCode());

            // 4. 轮询获取令牌 — 验证设备授权闭环
            ApiResponse<Map<String, Object>> tokenResp = oAuthController.token(TokenRequest.builder()
                    .grantType("urn:ietf:params:oauth:grant-type:device_code")
                    .clientId("vehicle-client").deviceCode(deviceResp.getDeviceCode()).build());
            assertEquals("000000", tokenResp.getCode());
            assertNotNull(tokenResp.getData().get("access_token"));
        }
    }

    // ========================================================================
    // 链路 5：OAuth 2.0 / OIDC
    // ========================================================================
    @Nested
    @DisplayName("链路 5：OAuth 2.0 / OIDC 闭环")
    class OAuthOidcLoop {

        OpenOAuthController oAuthController;
        OpenOidcController oidcController;

        @BeforeEach
        void init() {
            OAuthAuthorizationService oAuthAuthService = new OAuthAuthorizationService(
                    authCodeRepository, clientRepository, passwordEncoder);
            DeviceAuthorizationService deviceAuthService = new DeviceAuthorizationService(
                    clientRepository, verificationCodeStore);
            RefreshTokenDomainService refreshTokenService = new RefreshTokenDomainService(
                    refreshTokenRepository);
            JwtTokenService jwtTokenService = new JwtTokenService();

            oAuthController = new OpenOAuthController(oAuthAuthService, deviceAuthService,
                    refreshTokenService, jwtTokenService);

            OidcService oidcService = new OidcService(profileRepository, identityService, fieldEncryptor);
            oidcController = new OpenOidcController(oidcService, jwtTokenService);

            when(clientRepository.findByClientId("web-client"))
                    .thenReturn(Optional.of(stubClient("web-client", "public")));
        }

        @Test
        @DisplayName("Controller → DomainService：Authorization Code + PKCE 签发授权码")
        void authorizationCode_issueCode() {
            ApiResponse<Map<String, String>> response = oAuthController.authorize(AuthorizeRequest.builder()
                    .clientId("web-client").userId("U003").sessionId("session-001")
                    .redirectUri("https://app.example.com/callback").scope("openid,profile")
                    .codeChallenge("challenge123").challengeMethod("S256").build());
            assertEquals("000000", response.getCode());
            assertNotNull(response.getData().get("code"));
            assertEquals("https://app.example.com/callback", response.getData().get("redirectUri"));
            verify(authCodeRepository).insert(any());
        }

        @Test
        @DisplayName("Controller → DomainService：Client Credentials 签发令牌")
        void clientCredentials_issueToken() {
            String hashedSecret = passwordEncoder.encode("secret123");
            CiamOAuthClientDo confidentialClient = stubClient("service-client", "confidential");
            confidentialClient.setClientSecretHash(hashedSecret);
            confidentialClient.setPkceRequired(0);
            when(clientRepository.findByClientId("service-client"))
                    .thenReturn(Optional.of(confidentialClient));

            ApiResponse<Map<String, Object>> response = oAuthController.token(TokenRequest.builder()
                    .grantType("client_credentials").clientId("service-client")
                    .clientSecret("secret123").scope("openid").build());
            assertEquals("000000", response.getCode());
            assertNotNull(response.getData().get("access_token"));
        }

        @Test
        @DisplayName("Controller → DomainService：OIDC Discovery Document")
        void oidc_discoveryDocument() {
            ApiResponse<OidcDiscoveryDocument> response = oidcController.discoveryDocument();
            assertEquals("000000", response.getCode());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getIssuer());
        }

        @Test
        @DisplayName("Controller → DomainService：OIDC UserInfo")
        void oidc_userInfo() {
            when(profileRepository.findByUserId("U003")).thenReturn(Optional.empty());
            when(identityRepository.findByUserId("U003")).thenReturn(List.of());

            ApiResponse<OidcUserInfo> response = oidcController.userInfo("U003");
            assertEquals("000000", response.getCode());
            assertEquals("U003", response.getData().getSub());
        }

        @Test
        @DisplayName("Controller → DomainService：JWKS 端点")
        void oidc_jwks() {
            ApiResponse<Map<String, Object>> response = oidcController.jwks();
            assertEquals("000000", response.getCode());
            assertNotNull(response.getData().get("keys"));
        }
    }

    // ========================================================================
    // 链路 6：会话管理
    // ========================================================================
    @Nested
    @DisplayName("链路 6：会话管理闭环")
    class SessionManagementLoop {

        MobileAuthController authController;

        @BeforeEach
        void init() {
            authController = buildAuthController();
        }

        @Test
        @DisplayName("Controller → AppService → SessionDomainService：退出登录")
        void logout_invalidatesSession() {
            CiamSessionDo session = new CiamSessionDo();
            session.setSessionId("session-100");
            session.setUserId("U004");
            session.setSessionStatus(SessionStatus.ACTIVE.getCode());
            when(sessionRepository.findBySessionId("session-100")).thenReturn(Optional.of(session));

            ApiResponse<Void> response = authController.logout("app-client", LogoutRequest.builder()
                    .sessionId("session-100").userId("U004").build());
            assertEquals("000000", response.getCode());
            verify(sessionRepository).updateBySessionId(any());
            verify(refreshTokenRepository).revokeAllBySessionId("session-100");
        }

        @Test
        @DisplayName("SessionDomainService：查询用户活跃会话")
        void queryUserSessions() {
            when(sessionRepository.findByUserIdAndStatus("U004", SessionStatus.ACTIVE.getCode()))
                    .thenReturn(List.of(new CiamSessionDo()));

            List<CiamSessionDo> sessions = sessionService.findUserSessions("U004");
            assertFalse(sessions.isEmpty());
        }

        @Test
        @DisplayName("SessionDomainService：强制失效会话（密码修改场景）")
        void invalidateSession_forPasswordChange() {
            CiamSessionDo session = new CiamSessionDo();
            session.setSessionId("session-200");
            session.setUserId("U004");
            session.setSessionStatus(SessionStatus.ACTIVE.getCode());
            when(sessionRepository.findBySessionId("session-200")).thenReturn(Optional.of(session));

            sessionService.invalidateSession("session-200");
            verify(sessionRepository).updateBySessionId(argThat(s ->
                    s.getSessionStatus() == SessionStatus.INVALID.getCode()));
            verify(refreshTokenRepository).revokeAllBySessionId("session-200");
        }
    }

    // ========================================================================
    // 链路 7：MFA 多因素认证
    // ========================================================================
    @Nested
    @DisplayName("链路 7：MFA 多因素认证闭环")
    class MfaLoop {

        MobileRiskController riskController;
        MfaDomainService mfaDomainService;

        @BeforeEach
        void init() {
            mfaDomainService = new MfaDomainService(mfaChallengeRepository, smsAdapter, emailAdapter);
            riskController = new MobileRiskController(mfaDomainService, riskEventRepository);
        }

        @Test
        @DisplayName("Controller → MfaDomainService：触发 SMS MFA 挑战")
        void triggerSmsMfaChallenge() {
            ApiResponse<Map<String, String>> response = riskController.triggerMfa(TriggerMfaRequest.builder()
                    .userId("U005").sessionId("session-300").challengeType("sms")
                    .challengeScene("new_device").receiverMask("138****1234").build());
            assertEquals("000000", response.getCode());
            assertNotNull(response.getData().get("challengeId"));
            verify(mfaChallengeRepository).insert(any());
            verify(smsAdapter).sendVerificationCode(eq("138****1234"), eq("+86"), anyString());
        }

        @Test
        @DisplayName("Controller → MfaDomainService：校验 MFA 挑战通过")
        void verifyMfaChallenge_pass() {
            String correctCode = "123456";
            CiamMfaChallengeDo challenge = new CiamMfaChallengeDo();
            challenge.setChallengeId("ch-001");
            challenge.setUserId("U005");
            challenge.setChallengeStatus(ChallengeStatus.PENDING.getCode());
            challenge.setExpireTime(LocalDateTime.now().plusMinutes(5));
            challenge.setVerifyCodeHash(
                    net.hwyz.iov.cloud.sec.ciam.common.security.TokenDigest.fingerprint(correctCode));
            when(mfaChallengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            ApiResponse<Map<String, Boolean>> response = riskController.verifyMfa(VerifyMfaRequest.builder()
                    .challengeId("ch-001").code(correctCode).build());
            assertEquals("000000", response.getCode());
            assertTrue(response.getData().get("passed"));
            verify(mfaChallengeRepository).updateByChallengeId(argThat(c ->
                    c.getChallengeStatus() == ChallengeStatus.PASSED.getCode()));
        }

        @Test
        @DisplayName("Controller → MfaDomainService：校验 MFA 挑战失败")
        void verifyMfaChallenge_fail() {
            CiamMfaChallengeDo challenge = new CiamMfaChallengeDo();
            challenge.setChallengeId("ch-002");
            challenge.setUserId("U005");
            challenge.setChallengeStatus(ChallengeStatus.PENDING.getCode());
            challenge.setExpireTime(LocalDateTime.now().plusMinutes(5));
            challenge.setVerifyCodeHash(
                    net.hwyz.iov.cloud.sec.ciam.common.security.TokenDigest.fingerprint("654321"));
            when(mfaChallengeRepository.findByChallengeId("ch-002")).thenReturn(Optional.of(challenge));

            ApiResponse<Map<String, Boolean>> response = riskController.verifyMfa(VerifyMfaRequest.builder()
                    .challengeId("ch-002").code("000000").build());
            assertEquals("000000", response.getCode());
            assertFalse(response.getData().get("passed"));
            verify(mfaChallengeRepository).updateByChallengeId(argThat(c ->
                    c.getChallengeStatus() == ChallengeStatus.FAILED.getCode()));
        }

        @Test
        @DisplayName("MfaDomainService：取消待验证挑战")
        void cancelPendingChallenge() {
            CiamMfaChallengeDo challenge = new CiamMfaChallengeDo();
            challenge.setChallengeId("ch-003");
            challenge.setChallengeStatus(ChallengeStatus.PENDING.getCode());
            when(mfaChallengeRepository.findByChallengeId("ch-003")).thenReturn(Optional.of(challenge));

            mfaDomainService.cancelChallenge("ch-003");
            verify(mfaChallengeRepository).updateByChallengeId(argThat(c ->
                    c.getChallengeStatus() == ChallengeStatus.CANCELLED.getCode()));
        }
    }

    // ========================================================================
    // 链路 8：注销流程
    // ========================================================================
    @Nested
    @DisplayName("链路 8：注销流程闭环")
    class DeactivationLoop {

        AccountLifecycleAppService lifecycleAppService;

        @BeforeEach
        void init() {
            PasswordChangeAppService passwordChangeAppService = new PasswordChangeAppService(
                    credentialService, sessionRepository, refreshTokenRepository, auditLogger);

            // AccountLifecycleAppService 字段顺序（@RequiredArgsConstructor）:
            // verificationCodeService, identityDomainService, userDomainService,
            // passwordChangeAppService, deactivationRequestRepository,
            // userRepository, identityRepository, credentialRepository,
            // profileRepository, sessionRepository, refreshTokenRepository,
            // auditLogger, securityEventLogger
            lifecycleAppService = new AccountLifecycleAppService(
                    vcService, identityService, userService,
                    passwordChangeAppService, deactivationRequestRepository,
                    userRepository, identityRepository, credentialRepository,
                    profileRepository, sessionRepository, refreshTokenRepository,
                    auditLogger, securityEventLogger);
        }

        @Test
        @DisplayName("AppService → DomainService：提交注销申请")
        void submitDeactivationRequest() {
            when(userRepository.findByUserId("U006"))
                    .thenReturn(Optional.of(stubUser("U006", UserStatus.ACTIVE.getCode())));

            String requestId = lifecycleAppService.submitDeactivationRequest(
                    "U006", "app", "不再使用");
            assertNotNull(requestId);
            verify(deactivationRequestRepository).insert(any());
            verify(userRepository).updateByUserId(argThat(u ->
                    u.getUserStatus() == UserStatus.DEACTIVATING.getCode()));
        }

        @Test
        @DisplayName("AppService → DomainService：审核通过注销申请")
        void approveDeactivation() {
            CiamDeactivationRequestDo request = new CiamDeactivationRequestDo();
            request.setDeactivationRequestId("dr-001");
            request.setUserId("U006");
            request.setReviewStatus(0);
            request.setCheckStatus(1);
            request.setExecuteStatus(0);
            request.setRowValid(1);
            when(deactivationRequestRepository.findByDeactivationRequestId("dr-001"))
                    .thenReturn(Optional.of(request));

            lifecycleAppService.approveDeactivation("dr-001", "admin-001");
            verify(deactivationRequestRepository).updateByDeactivationRequestId(argThat(r ->
                    r.getReviewStatus() == 1));
        }

        @Test
        @DisplayName("AppService → DomainService：执行注销（物理删除核心数据）")
        void executeDeactivation() {
            CiamDeactivationRequestDo request = new CiamDeactivationRequestDo();
            request.setDeactivationRequestId("dr-002");
            request.setUserId("U007");
            request.setReviewStatus(1);
            request.setCheckStatus(1);
            request.setExecuteStatus(0);
            request.setRowValid(1);
            when(deactivationRequestRepository.findByDeactivationRequestId("dr-002"))
                    .thenReturn(Optional.of(request));
            when(userRepository.findByUserId("U007"))
                    .thenReturn(Optional.of(stubUser("U007", UserStatus.DEACTIVATING.getCode())));
            when(sessionRepository.findByUserIdAndStatus("U007", SessionStatus.ACTIVE.getCode()))
                    .thenReturn(List.of());

            lifecycleAppService.executeDeactivation("dr-002");

            verify(identityRepository).physicalDeleteByUserId("U007");
            verify(credentialRepository).physicalDeleteByUserId("U007");
            verify(userRepository).physicalDeleteByUserId("U007");
            verify(deactivationRequestRepository).updateByDeactivationRequestId(argThat(r ->
                    r.getExecuteStatus() == net.hwyz.iov.cloud.sec.ciam.domain.enums.ExecuteStatus.EXECUTED.getCode()));
        }
    }
}
