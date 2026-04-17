package net.hwyz.iov.cloud.sec.ciam.integration;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.application.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.application.LoginResult;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.MobileAuthController;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto.*;
import net.hwyz.iov.cloud.sec.ciam.controller.open.OpenOAuthController;
import net.hwyz.iov.cloud.sec.ciam.controller.open.dto.*;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AppleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.CaptchaAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.GoogleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.LocalMobileAuthAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.WechatLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.ClientType;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamDeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamSessionRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamSessionDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 任务 18.1：与手机 App、小程序、官网、车机完成接入联调。
 * <p>
 * 由于无法连接真实终端，本测试类模拟四种客户端类型（app、mini_program、web、vehicle）
 * 与 CIAM 系统的交互行为，覆盖以下场景：
 * <ol>
 *   <li>统一登录 — 各客户端类型通过同一 API 认证并获取有效令牌</li>
 *   <li>Token 使用 — Access Token 与 Refresh Token 对各客户端类型正确工作</li>
 *   <li>登出 — 各客户端类型可正确登出并使会话失效</li>
 *   <li>设备管理 — 同一用户可注册和管理多个设备</li>
 *   <li>多端并发 — 同一用户从多个客户端类型同时登录，会话相互隔离</li>
 * </ol>
 *
 * @see net.hwyz.iov.cloud.sec.ciam.domain.enums.ClientType
 */
@DisplayName("任务 18.1：多端接入联调验证")
class ClientIntegrationTest {

    // ---- 客户端类型常量 ----
    static final String[] CLIENT_TYPES = {
            ClientType.APP.getCode(),
            ClientType.MINI_PROGRAM.getCode(),
            ClientType.WEB.getCode(),
            ClientType.VEHICLE.getCode()
    };

    // ---- Mock 仓储 ----
    CiamUserRepository userRepository;
    CiamUserIdentityRepository identityRepository;
    CiamUserCredentialRepository credentialRepository;
    CiamUserProfileRepository profileRepository;
    CiamSessionRepository sessionRepository;
    CiamRefreshTokenRepository refreshTokenRepository;
    CiamDeviceRepository deviceRepository;
    CiamOAuthClientRepository clientRepository;
    CiamAuthCodeRepository authCodeRepository;

    // ---- Mock 适配器 ----
    SmsAdapter smsAdapter;
    EmailAdapter emailAdapter;
    WechatLoginAdapter wechatLoginAdapter;
    AppleLoginAdapter appleLoginAdapter;
    GoogleLoginAdapter googleLoginAdapter;
    LocalMobileAuthAdapter localMobileAuthAdapter;
    CaptchaAdapter captchaAdapter;
    AuditLogger auditLogger;

    // ---- 基础设施 ----
    FieldEncryptor fieldEncryptor;
    PasswordEncoder passwordEncoder;
    InMemoryVerificationCodeStore verificationCodeStore;

    // ---- 领域服务 ----
    VerificationCodeService vcService;
    IdentityDomainService identityService;
    UserDomainService userService;
    CredentialDomainService credentialService;
    SessionDomainService sessionService;
    PasswordPolicyService passwordPolicyService;
    CaptchaDomainService captchaService;
    JwtTokenService jwtTokenService;
    RefreshTokenDomainService refreshTokenService;
    OAuthAuthorizationService oAuthAuthorizationService;
    DeviceAuthorizationService deviceAuthorizationService;

    // ---- 控制器 ----
    MobileAuthController authController;
    OpenOAuthController oAuthController;

    // ---- 模拟会话存储 ----
    final Map<String, CiamSessionDo> sessionStore = new ConcurrentHashMap<>();
    final Map<String, CiamDeviceDo> deviceStore = new ConcurrentHashMap<>();

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

        // 适配器
        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        wechatLoginAdapter = mock(WechatLoginAdapter.class);
        appleLoginAdapter = mock(AppleLoginAdapter.class);
        googleLoginAdapter = mock(GoogleLoginAdapter.class);
        localMobileAuthAdapter = mock(LocalMobileAuthAdapter.class);
        captchaAdapter = mock(CaptchaAdapter.class);
        auditLogger = mock(AuditLogger.class);

        // 基础设施
        fieldEncryptor = new FieldEncryptor("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        passwordEncoder = new PasswordEncoder();
        verificationCodeStore = new InMemoryVerificationCodeStore();

        when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                .thenReturn(new AdapterResult(true, "ok"));
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(new AdapterResult(true, "ok"));

        // 领域服务
        passwordPolicyService = new PasswordPolicyService();
        vcService = new VerificationCodeService(verificationCodeStore, smsAdapter, emailAdapter);
        identityService = new IdentityDomainService(identityRepository, fieldEncryptor);
        userService = new UserDomainService(userRepository, profileRepository);
        credentialService = new CredentialDomainService(credentialRepository, passwordEncoder, passwordPolicyService);
        sessionService = new SessionDomainService(sessionRepository, refreshTokenRepository, deviceRepository);
        DeviceDomainService deviceService = new DeviceDomainService(deviceRepository);
        captchaService = new CaptchaDomainService(captchaAdapter, verificationCodeStore);
        jwtTokenService = new JwtTokenService();
        refreshTokenService = new RefreshTokenDomainService(refreshTokenRepository);
        oAuthAuthorizationService = new OAuthAuthorizationService(authCodeRepository, clientRepository, passwordEncoder);
        deviceAuthorizationService = new DeviceAuthorizationService(clientRepository, verificationCodeStore);

        // 控制器
        AuthenticationAppService authAppService = new AuthenticationAppService(
                vcService, identityService, userService,
                userRepository, auditLogger, credentialService, captchaService,
                sessionService, wechatLoginAdapter, appleLoginAdapter,
                googleLoginAdapter, localMobileAuthAdapter, jwtTokenService, deviceService);
        authController = new MobileAuthController(authAppService, vcService, captchaService);
        oAuthController = new OpenOAuthController(
                oAuthAuthorizationService, deviceAuthorizationService, refreshTokenService, jwtTokenService);

        // 清理模拟存储
        sessionStore.clear();
        deviceStore.clear();

        // 模拟会话仓储行为
        setupSessionRepositoryMocks();
        setupDeviceRepositoryMocks();
    }

    private void setupSessionRepositoryMocks() {
        doAnswer(inv -> {
            CiamSessionDo s = inv.getArgument(0);
            sessionStore.put(s.getSessionId(), s);
            return 1;
        }).when(sessionRepository).insert(any(CiamSessionDo.class));

        when(sessionRepository.findBySessionId(anyString())).thenAnswer(inv -> {
            String sid = inv.getArgument(0);
            return Optional.ofNullable(sessionStore.get(sid));
        });

        doAnswer(inv -> {
            CiamSessionDo s = inv.getArgument(0);
            sessionStore.put(s.getSessionId(), s);
            return 1;
        }).when(sessionRepository).updateBySessionId(any(CiamSessionDo.class));

        when(sessionRepository.findByUserIdAndStatus(anyString(), anyInt())).thenAnswer(inv -> {
            String userId = inv.getArgument(0);
            int status = inv.getArgument(1);
            return sessionStore.values().stream()
                    .filter(s -> userId.equals(s.getUserId()) && s.getSessionStatus() == status)
                    .toList();
        });

        when(sessionRepository.findByDeviceIdAndStatus(anyString(), anyInt())).thenAnswer(inv -> {
            String deviceId = inv.getArgument(0);
            int status = inv.getArgument(1);
            return sessionStore.values().stream()
                    .filter(s -> deviceId.equals(s.getDeviceId()) && s.getSessionStatus() == status)
                    .toList();
        });
    }

    private void setupDeviceRepositoryMocks() {
        doAnswer(inv -> {
            CiamDeviceDo d = inv.getArgument(0);
            deviceStore.put(d.getDeviceId(), d);
            return 1;
        }).when(deviceRepository).insert(any(CiamDeviceDo.class));

        when(deviceRepository.findByDeviceId(anyString())).thenAnswer(inv -> {
            String did = inv.getArgument(0);
            return Optional.ofNullable(deviceStore.get(did));
        });

        doAnswer(inv -> {
            CiamDeviceDo d = inv.getArgument(0);
            deviceStore.put(d.getDeviceId(), d);
            return 1;
        }).when(deviceRepository).updateByDeviceId(any(CiamDeviceDo.class));

        when(deviceRepository.findByUserIdAndStatus(anyString(), anyInt())).thenAnswer(inv -> {
            String userId = inv.getArgument(0);
            int status = inv.getArgument(1);
            return deviceStore.values().stream()
                    .filter(d -> userId.equals(d.getUserId()) && d.getDeviceStatus() == status)
                    .toList();
        });
    }

    // ---- 辅助方法 ----

    private CiamUserIdentityDo stubIdentity(String userId, String type, String value) {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("id-" + userId + "-" + type);
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

    private CiamOAuthClientDo stubClient(String clientId) {
        CiamOAuthClientDo client = new CiamOAuthClientDo();
        client.setClientId(clientId);
        client.setClientName("Test Client");
        client.setClientType("public");
        client.setGrantTypes("authorization_code,client_credentials,refresh_token,urn:ietf:params:oauth:grant-type:device_code");
        client.setScopes("openid,profile,email");
        client.setRedirectUris("https://app.example.com/callback");
        client.setPkceRequired(0);
        client.setAccessTokenTtl(1800);
        client.setRefreshTokenTtl(2592000);
        client.setClientStatus(1);
        client.setRowValid(1);
        return client;
    }

    private void stubExistingUser(String userId, String mobile) {
        CiamUserIdentityDo identity = stubIdentity(userId, "mobile", mobile);
        CiamUserDo user = stubUser(userId, UserStatus.ACTIVE.getCode());

        when(identityRepository.findByTypeAndHash(eq("mobile"), eq(FieldEncryptor.hash(mobile))))
                .thenReturn(Optional.of(identity));
        when(userRepository.findByUserId(eq(userId)))
                .thenReturn(Optional.of(user));
    }

    private CiamSessionDo createActiveSession(String userId, String sessionId,
                                               String clientType, String deviceId) {
        CiamSessionDo session = new CiamSessionDo();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setClientType(clientType);
        session.setDeviceId(deviceId);
        session.setSessionStatus(SessionStatus.ACTIVE.getCode());
        session.setLoginTime(Instant.now());
        session.setLastActiveTime(Instant.now());
        session.setExpireTime(Instant.now().plusSeconds(24 * 3600));
        session.setRowValid(1);
        sessionStore.put(sessionId, session);
        return session;
    }

    private CiamDeviceDo createActiveDevice(String userId, String deviceId, String clientType) {
        CiamDeviceDo device = new CiamDeviceDo();
        device.setDeviceId(deviceId);
        device.setUserId(userId);
        device.setClientType(clientType);
        device.setDeviceName(clientType + " device");
        device.setDeviceStatus(DeviceStatus.ACTIVE.getCode());
        device.setFirstLoginTime(Instant.now());
        device.setLastLoginTime(Instant.now());
        device.setRowValid(1);
        deviceStore.put(deviceId, device);
        return device;
    }

    // ========================================================================
    // 1. 统一登录 — 各客户端类型通过同一 API 认证并获取有效令牌
    // ========================================================================

    @Nested
    @DisplayName("1. 统一登录：各客户端类型通过同一 API 认证")
    class UnifiedLoginTests {

        @Test
        @DisplayName("手机 App 通过手机号验证码登录成功")
        void appLoginByMobileCode() {
            String mobile = "13800000001";
            String userId = "user-app-001";
            stubExistingUser(userId, mobile);

            // 发送验证码
            authController.sendMobileCode("client-app", SendMobileCodeRequest.builder().mobile(mobile).countryCode("86").build());
            // 获取验证码
            String code = verificationCodeStore.getCode("sms:" + mobile).orElseThrow();

            ApiResponse<LoginResult> response = authController.loginByMobile("client-app", "device-001", "iOS", "iOS", "1.0.0", MobileLoginRequest.builder().mobile(mobile).countryCode("86").code(code).build());

            assertEquals("000000", response.getCode());
            assertNotNull(response.getData());
            assertEquals(userId, response.getData().getUserId());
        }

        @Test
        @DisplayName("小程序通过手机号验证码登录成功")
        void miniProgramLoginByMobileCode() {
            String mobile = "13800000002";
            String userId = "user-mini-001";
            stubExistingUser(userId, mobile);

            authController.sendMobileCode("client-mini", SendMobileCodeRequest.builder().mobile(mobile).countryCode("86").build());
            String code = verificationCodeStore.getCode("sms:" + mobile).orElseThrow();

            ApiResponse<LoginResult> response = authController.loginByMobile("client-mini", "device-002", "mini_program", "WeChat", "1.0.0", MobileLoginRequest.builder().mobile(mobile).countryCode("86").code(code).build());

            assertEquals("000000", response.getCode());
            assertEquals(userId, response.getData().getUserId());
        }

        @Test
        @DisplayName("官网通过手机号验证码登录成功")
        void webLoginByMobileCode() {
            String mobile = "13800000003";
            String userId = "user-web-001";
            stubExistingUser(userId, mobile);

            authController.sendMobileCode("client-web", SendMobileCodeRequest.builder().mobile(mobile).countryCode("86").build());
            String code = verificationCodeStore.getCode("sms:" + mobile).orElseThrow();

            ApiResponse<LoginResult> response = authController.loginByMobile("client-web", "device-003", "web", "Chrome", "1.0.0", MobileLoginRequest.builder().mobile(mobile).countryCode("86").code(code).build());

            assertEquals("000000", response.getCode());
            assertEquals(userId, response.getData().getUserId());
        }

        @Test
        @DisplayName("同一用户在不同客户端登录返回相同 userId")
        void sameUserSameIdAcrossClients() {
            String mobile = "13800000010";
            String userId = "user-unified-001";
            stubExistingUser(userId, mobile);

            String[] clientIds = {"client-app", "client-mini", "client-web"};
            for (String clientId : clientIds) {
                authController.sendMobileCode(clientId, SendMobileCodeRequest.builder().mobile(mobile).countryCode("86").build());
                String code = verificationCodeStore.getCode("sms:" + mobile).orElseThrow();
                ApiResponse<LoginResult> response = authController.loginByMobile(
                        clientId, "device-004", "multi", "multi", "1.0.0", MobileLoginRequest.builder().mobile(mobile).countryCode("86").code(code).build());

                assertEquals("000000", response.getCode(), "登录失败: clientId=" + clientId);
                assertEquals(userId, response.getData().getUserId(),
                        "不同客户端应返回相同 userId: clientId=" + clientId);
            }
        }
    }

    // ========================================================================
    // 2. Token 使用 — Access Token 与 Refresh Token 对各客户端类型正确工作
    // ========================================================================

    @Nested
    @DisplayName("2. Token 使用：Access Token 签发与校验")
    class TokenUsageTests {

        @Test
        @DisplayName("为各客户端类型签发的 Access Token 均可正确校验")
        void accessTokenValidForAllClientTypes() {
            String userId = "user-token-001";

            for (String clientType : CLIENT_TYPES) {
                String clientId = "client-" + clientType;
                String sessionId = "session-" + clientType;

                String accessToken = jwtTokenService.generateAccessToken(
                        userId, clientId, "openid profile", sessionId, 1800);

                assertNotNull(accessToken, "Access Token 不应为空: " + clientType);

                TokenClaims claims = jwtTokenService.validateAccessToken(accessToken);
                assertEquals(userId, claims.getSub(), "sub 应为 userId: " + clientType);
                assertEquals(clientId, claims.getClientId(), "client_id 应匹配: " + clientType);
                assertEquals(sessionId, claims.getSessionId(), "session_id 应匹配: " + clientType);
                assertEquals("openid profile", claims.getScope(), "scope 应匹配: " + clientType);
            }
        }

        @Test
        @DisplayName("不同客户端的 Access Token 包含各自独立的 session_id")
        void accessTokensContainDistinctSessionIds() {
            String userId = "user-token-002";
            List<String> sessionIds = new ArrayList<>();

            for (String clientType : CLIENT_TYPES) {
                String sessionId = "session-" + clientType + "-" + System.nanoTime();
                sessionIds.add(sessionId);

                String token = jwtTokenService.generateAccessToken(
                        userId, "client-" + clientType, "openid", sessionId, 1800);
                TokenClaims claims = jwtTokenService.validateAccessToken(token);
                assertEquals(sessionId, claims.getSessionId());
            }

            // 确保所有 session_id 互不相同
            assertEquals(CLIENT_TYPES.length, sessionIds.stream().distinct().count(),
                    "各客户端的 session_id 应互不相同");
        }

        @Test
        @DisplayName("车机通过 Device Authorization Grant 获取有效 Token")
        void vehicleDeviceAuthorizationGrantToken() {
            String clientId = "client-vehicle";
            String userId = "user-vehicle-001";
            when(clientRepository.findByClientId(eq(clientId)))
                    .thenReturn(Optional.of(stubClient(clientId)));

            // 发起设备授权
            ApiResponse<DeviceAuthorizationResponse> deviceResp =
                    oAuthController.deviceAuthorize(DeviceAuthorizeRequest.builder().clientId(clientId).scope("openid").build());
            assertEquals("000000", deviceResp.getCode());
            String userCode = deviceResp.getData().getUserCode();
            String deviceCode = deviceResp.getData().getDeviceCode();

            // 用户确认
            oAuthController.approveDevice(ApproveDeviceRequest.builder().userCode(userCode).userId(userId).build());

            // 轮询获取令牌
            ApiResponse<Map<String, Object>> tokenResp = oAuthController.token(
                    TokenRequest.builder().grantType("urn:ietf:params:oauth:grant-type:device_code").clientId(clientId).deviceCode(deviceCode).build());

            assertEquals("000000", tokenResp.getCode());
            assertNotNull(tokenResp.getData().get("access_token"));

            // 校验签发的 Access Token
            String accessToken = (String) tokenResp.getData().get("access_token");
            TokenClaims claims = jwtTokenService.validateAccessToken(accessToken);
            assertEquals(userId, claims.getSub());
            assertEquals(clientId, claims.getClientId());
        }
    }

    // ========================================================================
    // 3. 登出 — 各客户端类型可正确登出并使会话失效
    // ========================================================================

    @Nested
    @DisplayName("3. 登出：各客户端类型正确登出")
    class LogoutTests {

        @Test
        @DisplayName("各客户端类型独立登出，仅影响自身会话")
        void logoutOnlyAffectsOwnSession() {
            String userId = "user-logout-001";

            // 为每种客户端创建活跃会话
            for (String clientType : CLIENT_TYPES) {
                createActiveSession(userId, "session-" + clientType, clientType, "device-" + clientType);
            }

            // App 端登出
            sessionService.logout("session-app", userId);

            // 验证 App 会话已下线
            CiamSessionDo appSession = sessionStore.get("session-app");
            assertEquals(SessionStatus.KICKED.getCode(), appSession.getSessionStatus(),
                    "App 会话应已下线");

            // 验证其他端会话仍然活跃
            for (String clientType : new String[]{"mini_program", "web", "vehicle"}) {
                CiamSessionDo session = sessionStore.get("session-" + clientType);
                assertEquals(SessionStatus.ACTIVE.getCode(), session.getSessionStatus(),
                        clientType + " 会话应仍然活跃");
            }
        }

        @Test
        @DisplayName("登出后 Refresh Token 被撤销")
        void logoutRevokesRefreshTokens() {
            String userId = "user-logout-002";
            String sessionId = "session-logout-rt";
            createActiveSession(userId, sessionId, "app", "device-app-rt");

            sessionService.logout(sessionId, userId);

            verify(refreshTokenRepository).revokeAllBySessionId(eq(sessionId));
        }

        @Test
        @DisplayName("登出非自身会话抛出 FORBIDDEN")
        void logoutOtherUserSessionThrowsForbidden() {
            String userId = "user-logout-003";
            String otherUserId = "user-logout-004";
            createActiveSession(userId, "session-other", "app", "device-other");

            assertThrows(BusinessException.class,
                    () -> sessionService.logout("session-other", otherUserId),
                    "登出他人会话应抛出异常");
        }
    }

    // ========================================================================
    // 4. 设备管理 — 多设备注册与管理
    // ========================================================================

    @Nested
    @DisplayName("4. 设备管理：多设备注册与管理")
    class DeviceManagementTests {

        @Test
        @DisplayName("同一用户可拥有多个不同客户端类型的设备")
        void userCanHaveMultipleDevices() {
            String userId = "user-device-001";

            for (String clientType : CLIENT_TYPES) {
                createActiveDevice(userId, "device-" + clientType, clientType);
            }

            List<CiamDeviceDo> devices = sessionService.findUserDevices(userId);
            assertEquals(CLIENT_TYPES.length, devices.size(),
                    "用户应拥有 " + CLIENT_TYPES.length + " 个设备");
        }

        @Test
        @DisplayName("强制下线指定设备会使该设备上的所有会话失效")
        void kickDeviceInvalidatesAllDeviceSessions() {
            String userId = "user-device-002";
            String deviceId = "device-kick-target";

            createActiveDevice(userId, deviceId, "app");
            createActiveSession(userId, "session-d1", "app", deviceId);
            createActiveSession(userId, "session-d2", "app", deviceId);
            // 另一设备上的会话
            createActiveDevice(userId, "device-other", "web");
            createActiveSession(userId, "session-other", "web", "device-other");

            sessionService.kickDevice(deviceId, userId);

            // 被下线设备上的会话应失效
            assertEquals(SessionStatus.KICKED.getCode(),
                    sessionStore.get("session-d1").getSessionStatus());
            assertEquals(SessionStatus.KICKED.getCode(),
                    sessionStore.get("session-d2").getSessionStatus());

            // 其他设备上的会话不受影响
            assertEquals(SessionStatus.ACTIVE.getCode(),
                    sessionStore.get("session-other").getSessionStatus());

            // 设备状态应为失效
            assertEquals(DeviceStatus.INVALID.getCode(),
                    deviceStore.get(deviceId).getDeviceStatus());
        }

        @Test
        @DisplayName("不能下线他人设备")
        void cannotKickOtherUserDevice() {
            String userId = "user-device-003";
            String otherUserId = "user-device-004";
            createActiveDevice(userId, "device-owned", "app");

            assertThrows(BusinessException.class,
                    () -> sessionService.kickDevice("device-owned", otherUserId));
        }

        @Test
        @DisplayName("查询用户设备列表仅返回活跃设备")
        void findUserDevicesReturnsOnlyActive() {
            String userId = "user-device-005";
            createActiveDevice(userId, "device-active-1", "app");
            createActiveDevice(userId, "device-active-2", "web");

            // 创建一个失效设备
            CiamDeviceDo invalidDevice = createActiveDevice(userId, "device-invalid", "mini_program");
            invalidDevice.setDeviceStatus(DeviceStatus.INVALID.getCode());

            List<CiamDeviceDo> devices = sessionService.findUserDevices(userId);
            assertEquals(2, devices.size(), "应仅返回活跃设备");
        }
    }

    // ========================================================================
    // 5. 多端并发 — 同一用户多客户端同时登录，会话隔离
    // ========================================================================

    @Nested
    @DisplayName("5. 多端并发：同一用户多客户端同时登录")
    class MultiClientConcurrencyTests {

        @Test
        @DisplayName("同一用户可在四种客户端同时拥有活跃会话")
        void userCanHaveConcurrentSessionsAcrossAllClientTypes() {
            String userId = "user-concurrent-001";

            for (String clientType : CLIENT_TYPES) {
                createActiveSession(userId, "session-c-" + clientType, clientType, "device-c-" + clientType);
            }

            List<CiamSessionDo> activeSessions = sessionService.findUserSessions(userId);
            assertEquals(CLIENT_TYPES.length, activeSessions.size(),
                    "用户应在所有客户端类型上拥有活跃会话");

            // 验证每种客户端类型都有一个会话
            for (String clientType : CLIENT_TYPES) {
                boolean found = activeSessions.stream()
                        .anyMatch(s -> clientType.equals(s.getClientType()));
                assertTrue(found, "应存在 " + clientType + " 类型的活跃会话");
            }
        }

        @Test
        @DisplayName("下线一个客户端不影响其他客户端的会话")
        void kickOneClientDoesNotAffectOthers() {
            String userId = "user-concurrent-002";

            for (String clientType : CLIENT_TYPES) {
                createActiveSession(userId, "session-iso-" + clientType, clientType, "device-iso-" + clientType);
            }

            // 下线小程序端
            sessionService.logout("session-iso-mini_program", userId);

            // 小程序会话已下线
            assertEquals(SessionStatus.KICKED.getCode(),
                    sessionStore.get("session-iso-mini_program").getSessionStatus());

            // 其他端仍然活跃
            assertEquals(SessionStatus.ACTIVE.getCode(),
                    sessionStore.get("session-iso-app").getSessionStatus());
            assertEquals(SessionStatus.ACTIVE.getCode(),
                    sessionStore.get("session-iso-web").getSessionStatus());
            assertEquals(SessionStatus.ACTIVE.getCode(),
                    sessionStore.get("session-iso-vehicle").getSessionStatus());

            // 剩余活跃会话数量为 3
            List<CiamSessionDo> remaining = sessionService.findUserSessions(userId);
            assertEquals(3, remaining.size());
        }

        @Test
        @DisplayName("各客户端的 Access Token 包含独立的 session_id 和 client_id")
        void concurrentTokensAreIsolated() {
            String userId = "user-concurrent-003";
            Map<String, String> tokenMap = new ConcurrentHashMap<>();

            for (String clientType : CLIENT_TYPES) {
                String clientId = "client-" + clientType;
                String sessionId = "session-t-" + clientType;
                String token = jwtTokenService.generateAccessToken(
                        userId, clientId, "openid", sessionId, 1800);
                tokenMap.put(clientType, token);
            }

            // 校验每个 Token 的声明互相独立
            for (String clientType : CLIENT_TYPES) {
                TokenClaims claims = jwtTokenService.validateAccessToken(tokenMap.get(clientType));
                assertEquals(userId, claims.getSub(), "所有 Token 的 sub 应相同");
                assertEquals("client-" + clientType, claims.getClientId(),
                        clientType + " Token 的 client_id 应匹配");
                assertEquals("session-t-" + clientType, claims.getSessionId(),
                        clientType + " Token 的 session_id 应匹配");
            }
        }

        @Test
        @DisplayName("强制失效指定会话不影响同一用户的其他会话")
        void invalidateSessionDoesNotAffectOtherSessions() {
            String userId = "user-concurrent-004";

            createActiveSession(userId, "session-inv-app", "app", "device-inv-app");
            createActiveSession(userId, "session-inv-web", "web", "device-inv-web");

            // 强制失效 App 会话（模拟风险事件触发）
            sessionService.invalidateSession("session-inv-app");

            assertEquals(SessionStatus.INVALID.getCode(),
                    sessionStore.get("session-inv-app").getSessionStatus(),
                    "App 会话应被强制失效");
            assertEquals(SessionStatus.ACTIVE.getCode(),
                    sessionStore.get("session-inv-web").getSessionStatus(),
                    "Web 会话应不受影响");
        }

        @Test
        @DisplayName("同一用户同一客户端类型可拥有多个会话（多设备）")
        void sameClientTypeMultipleDeviceSessions() {
            String userId = "user-concurrent-005";

            createActiveSession(userId, "session-app-phone1", "app", "device-phone1");
            createActiveSession(userId, "session-app-phone2", "app", "device-phone2");
            createActiveSession(userId, "session-web-1", "web", "device-web-1");

            List<CiamSessionDo> sessions = sessionService.findUserSessions(userId);
            assertEquals(3, sessions.size(), "应有 3 个活跃会话");

            long appSessions = sessions.stream()
                    .filter(s -> "app".equals(s.getClientType()))
                    .count();
            assertEquals(2, appSessions, "App 类型应有 2 个会话");
        }
    }
}
