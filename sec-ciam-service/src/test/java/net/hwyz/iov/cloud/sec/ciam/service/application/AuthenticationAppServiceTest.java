package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.LoginResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.cmd.LoginByMobileCodeCmd;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AppleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.GoogleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.LocalMobileAuthAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.WechatLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OAuthClient;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.config.CiamAuthenticationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 手机认证应用服务测试（SEC-CIAM-DSN-CR-003 §5.1/§5.2/§5.3，TC-003-1/4/5/6/8）。
 * <p>
 * 本类替换历史占位测试 {@code test_compilation}：断言真实编排语义
 * （客户端准入、事务内持久化顺序、完整 Token 上下文、刷新上下文恢复、登出委托）。
 */
class AuthenticationAppServiceTest {

    private static final String MOBILE = "13800138000";
    private static final String DEVICE_ID = "device-001";
    private static final String CLIENT_ID = "ciam-mobile-app";

    private VerificationCodeService verificationCodeService;
    private IdentityDomainService identityDomainService;
    private UserDomainService userDomainService;
    private UserRepository userRepository;
    private AuditLogger auditLogger;
    private CredentialDomainService credentialDomainService;
    private CaptchaDomainService captchaDomainService;
    private SessionDomainService sessionDomainService;
    private JwtTokenService jwtTokenService;
    private RefreshTokenDomainService refreshTokenDomainService;
    private DeviceDomainService deviceDomainService;
    private OAuthClientDomainService oauthClientDomainService;
    private CiamAuthenticationProperties authenticationProperties;

    private AuthenticationAppService service;

    @BeforeEach
    void setUp() {
        verificationCodeService = mock(VerificationCodeService.class);
        identityDomainService = mock(IdentityDomainService.class);
        userDomainService = mock(UserDomainService.class);
        userRepository = mock(UserRepository.class);
        auditLogger = mock(AuditLogger.class);
        credentialDomainService = mock(CredentialDomainService.class);
        captchaDomainService = mock(CaptchaDomainService.class);
        sessionDomainService = mock(SessionDomainService.class);
        jwtTokenService = mock(JwtTokenService.class);
        refreshTokenDomainService = mock(RefreshTokenDomainService.class);
        deviceDomainService = mock(DeviceDomainService.class);
        oauthClientDomainService = mock(OAuthClientDomainService.class);

        authenticationProperties = new CiamAuthenticationProperties();
        authenticationProperties.getMobile().setAllowedClientIds(List.of(CLIENT_ID));

        service = new AuthenticationAppService(
                verificationCodeService, identityDomainService, userDomainService, userRepository,
                auditLogger, credentialDomainService, captchaDomainService, sessionDomainService,
                mock(WechatLoginAdapter.class), mock(AppleLoginAdapter.class),
                mock(GoogleLoginAdapter.class), mock(LocalMobileAuthAdapter.class),
                jwtTokenService, refreshTokenDomainService, deviceDomainService,
                oauthClientDomainService, authenticationProperties,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
    }

    private OAuthClient mobileClient() {
        return OAuthClient.builder()
                .clientId(CLIENT_ID)
                .clientType("public")
                .scopes("openid profile phone")
                .accessTokenTtl(1800)
                .refreshTokenTtl(2592000)
                .clientStatus(1)
                .build();
    }

    private DeviceInfoDto deviceInfo() {
        return DeviceInfoDto.builder()
                .clientId(CLIENT_ID)
                .clientType("MOBILE")
                .deviceId(DEVICE_ID)
                .deviceOs("iOS")
                .appVersion("1.0.0")
                .build();
    }

    private LoginByMobileCodeCmd cmd() {
        return LoginByMobileCodeCmd.builder()
                .mobile(MOBILE)
                .countryCode("+86")
                .code("111111")
                .deviceId(DEVICE_ID)
                .deviceInfo(deviceInfo())
                .build();
    }

    private void stubClient() {
        when(oauthClientDomainService.requireMobileClient(eq(CLIENT_ID), any()))
                .thenReturn(mobileClient());
    }

    private Session stubSession(String userId, Instant expireTime) {
        Session session = Session.builder()
                .sessionId("session-001")
                .userId(userId)
                .clientId(CLIENT_ID)
                .clientType("MOBILE")
                .deviceId(DEVICE_ID)
                .scope("openid profile phone")
                .sessionStatus(SessionStatus.ACTIVE.getCode())
                .loginTime(Instant.now())
                .expireTime(expireTime)
                .build();
        when(sessionDomainService.openSession(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(session);
        return session;
    }

    @Nested
    class MobileLoginTests {

        @Test
        void newUserLogin_persistsCompleteSessionAndIssuesTokensWithFullContext() {
            stubClient();
            when(identityDomainService.findByTypeAndValue(IdentityType.MOBILE, MOBILE))
                    .thenReturn(Optional.empty());
            when(userDomainService.createUser(any(), any(), any(), any()))
                    .thenReturn(User.builder().userId("user-001").userStatus(UserStatus.PENDING.getCode()).build());
            when(identityDomainService.bindIdentity(anyString(), any(), anyString(), any(), anyString()))
                    .thenReturn(UserIdentity.builder().userId("user-001").identityHash("hash-001").build());

            Instant before = Instant.now();
            Session session = stubSession("user-001", before.plusSeconds(2592000));
            when(refreshTokenDomainService.issueRefreshToken(anyString(), anyString(), anyString(), any(Instant.class)))
                    .thenReturn("refresh-token");
            when(jwtTokenService.generateAccessToken(any(AccessTokenContext.class)))
                    .thenReturn("access-token");

            LoginResultDto result = service.loginByMobileCode(cmd());

            assertTrue(result.isNewUser());
            assertEquals("user-001", result.getUserId());
            assertEquals("access-token", result.getAccessToken());
            assertEquals("refresh-token", result.getRefreshToken());
            assertEquals("session-001", result.getSessionId());
            assertEquals(1800, result.getAccessTokenTtl());

            // 会话冻结客户端 scope / clientType / deviceId
            verify(sessionDomainService).openSession(eq("user-001"), eq(CLIENT_ID), eq("MOBILE"),
                    eq(DEVICE_ID), eq("openid profile phone"), any(Instant.class));
            // Refresh Token 与会话共享绝对到期边界
            verify(refreshTokenDomainService).issueRefreshToken("user-001", "session-001", CLIENT_ID,
                    session.getExpireTime());

            ArgumentCaptor<AccessTokenContext> captor = ArgumentCaptor.forClass(AccessTokenContext.class);
            verify(jwtTokenService).generateAccessToken(captor.capture());
            AccessTokenContext context = captor.getValue();
            assertEquals("user-001", context.getUserId());
            assertEquals(CLIENT_ID, context.getClientId());
            assertEquals("MOBILE", context.getClientType());
            assertEquals(DEVICE_ID, context.getDeviceId());
            assertEquals("session-001", context.getSessionId());
            assertEquals("openid profile phone", context.getScope());
            assertEquals(1800, context.getTtlSeconds());
            assertEquals(session.getExpireTime(), context.getExpireBound());
        }

        @Test
        void existingActiveUserLogin_doesNotCreateUser() {
            stubClient();
            when(identityDomainService.findByTypeAndValue(IdentityType.MOBILE, MOBILE))
                    .thenReturn(Optional.of(UserIdentity.builder()
                            .userId("user-002")
                            .identityStatus(IdentityStatus.BOUND.getCode())
                            .build()));
            when(userRepository.findByUserId("user-002"))
                    .thenReturn(Optional.of(User.builder()
                            .userId("user-002").userStatus(UserStatus.ACTIVE.getCode()).build()));
            stubSession("user-002", Instant.now().plusSeconds(2592000));
            when(refreshTokenDomainService.issueRefreshToken(anyString(), anyString(), anyString(), any(Instant.class)))
                    .thenReturn("refresh-token");
            when(jwtTokenService.generateAccessToken(any(AccessTokenContext.class))).thenReturn("access-token");

            LoginResultDto result = service.loginByMobileCode(cmd());

            assertFalse(result.isNewUser());
            assertEquals("user-002", result.getUserId());
            verify(userDomainService, never()).createUser(any(), any(), any(), any());
            verify(identityDomainService, never()).bindIdentity(anyString(), any(), anyString(), any(), anyString());
        }

        @Test
        void lockedUser_isRejectedWithoutSessionOrTokens() {
            stubClient();
            when(identityDomainService.findByTypeAndValue(IdentityType.MOBILE, MOBILE))
                    .thenReturn(Optional.of(UserIdentity.builder()
                            .userId("user-003")
                            .identityStatus(IdentityStatus.BOUND.getCode())
                            .build()));
            when(userRepository.findByUserId("user-003"))
                    .thenReturn(Optional.of(User.builder()
                            .userId("user-003").userStatus(UserStatus.LOCKED.getCode()).build()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(cmd()));

            assertEquals(CiamErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
            verify(sessionDomainService, never()).openSession(any(), any(), any(), any(), any(), any());
            verify(refreshTokenDomainService, never()).issueRefreshToken(anyString(), anyString(), anyString(), any(Instant.class));
            verify(jwtTokenService, never()).generateAccessToken(any(AccessTokenContext.class));
        }

        @Test
        void clientOutsideAllowlist_isRejectedBeforeVerificationCode() {
            when(oauthClientDomainService.requireMobileClient(eq(CLIENT_ID), any()))
                    .thenThrow(new BusinessException(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(cmd()));

            assertEquals(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED, ex.getErrorCode());
            verify(verificationCodeService, never()).verifyCode(anyString(), anyString(), any(), anyString());
        }

        @Test
        void missingDeviceInfo_isRejectedAsInvalidParam() {
            LoginByMobileCodeCmd noDevice = cmd();
            noDevice.setDeviceInfo(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(noDevice));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verify(oauthClientDomainService, never()).requireMobileClient(anyString(), any());
        }

        @Test
        void nonMobileClientType_isRejected() {
            LoginByMobileCodeCmd wrongType = cmd();
            wrongType.getDeviceInfo().setClientType("app");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(wrongType));

            assertEquals(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED, ex.getErrorCode());
        }

        @Test
        void invalidVerificationCode_writesNothing() {
            stubClient();
            doThrow(new BusinessException(CiamErrorCode.VERIFICATION_CODE_INVALID))
                    .when(verificationCodeService).verifyCode(anyString(), anyString(), any(), anyString());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(cmd()));

            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());
            verify(identityDomainService, never()).findByTypeAndValue(any(), anyString());
            verify(sessionDomainService, never()).openSession(any(), any(), any(), any(), any(), any());
        }

        @Test
        void concurrentFirstLoginUniqueConflict_mapsToIdentityConflictWithoutPartialSuccess() {
            stubClient();
            when(identityDomainService.findByTypeAndValue(IdentityType.MOBILE, MOBILE))
                    .thenReturn(Optional.empty());
            when(userDomainService.createUser(any(), any(), any(), any()))
                    .thenReturn(User.builder().userId("user-004").userStatus(UserStatus.PENDING.getCode()).build());
            when(identityDomainService.bindIdentity(anyString(), any(), anyString(), any(), anyString()))
                    .thenThrow(new DuplicateKeyException("uk_identity_type_hash_valid"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loginByMobileCode(cmd()));

            assertEquals(CiamErrorCode.IDENTITY_CONFLICT, ex.getErrorCode());
            verify(sessionDomainService, never()).openSession(any(), any(), any(), any(), any(), any());
            verify(refreshTokenDomainService, never()).issueRefreshToken(anyString(), anyString(), anyString(), any(Instant.class));
        }
    }

    @Nested
    class RefreshTokenTests {

        private RefreshToken activeToken() {
            return RefreshToken.builder()
                    .refreshTokenId("rt-001")
                    .userId("user-001")
                    .sessionId("session-001")
                    .clientId(CLIENT_ID)
                    .tokenStatus(1)
                    .expireTime(Instant.now().plusSeconds(86400))
                    .build();
        }

        private Session activeSession(String scope, Instant expireTime) {
            return Session.builder()
                    .sessionId("session-001")
                    .userId("user-001")
                    .clientId(CLIENT_ID)
                    .clientType("MOBILE")
                    .deviceId(DEVICE_ID)
                    .scope(scope)
                    .sessionStatus(SessionStatus.ACTIVE.getCode())
                    .expireTime(expireTime)
                    .build();
        }

        @Test
        void refresh_restoresFullContextFromPersistedSession() {
            stubClient();
            RefreshToken token = activeToken();
            Session session = activeSession("openid profile phone", Instant.now().plusSeconds(86400));
            when(refreshTokenDomainService.loadForRotation("raw-token", CLIENT_ID)).thenReturn(token);
            when(sessionDomainService.requireActiveSession("session-001")).thenReturn(session);
            when(userRepository.findByUserId("user-001"))
                    .thenReturn(Optional.of(User.builder()
                            .userId("user-001").userStatus(UserStatus.ACTIVE.getCode()).build()));
            when(refreshTokenDomainService.rotate(token, session))
                    .thenReturn(new RefreshTokenRotationResult("new-refresh-token", "user-001",
                            "session-001", CLIENT_ID, "MOBILE", DEVICE_ID,
                            "openid profile phone", token.getExpireTime()));
            when(jwtTokenService.generateAccessToken(any(AccessTokenContext.class)))
                    .thenReturn("new-access-token");

            LoginResultDto result = service.refreshToken("raw-token", CLIENT_ID);

            assertEquals("new-access-token", result.getAccessToken());
            assertEquals("new-refresh-token", result.getRefreshToken());
            assertEquals("session-001", result.getSessionId());
            assertFalse(result.isNewUser());

            ArgumentCaptor<AccessTokenContext> captor = ArgumentCaptor.forClass(AccessTokenContext.class);
            verify(jwtTokenService).generateAccessToken(captor.capture());
            AccessTokenContext context = captor.getValue();
            assertEquals("MOBILE", context.getClientType());
            assertEquals(DEVICE_ID, context.getDeviceId());
            assertEquals("openid profile phone", context.getScope());
            assertEquals(token.getExpireTime(), context.getExpireBound());
        }

        @Test
        void refresh_legacySessionWithoutScope_isRejected() {
            stubClient();
            RefreshToken token = activeToken();
            when(refreshTokenDomainService.loadForRotation("raw-token", CLIENT_ID)).thenReturn(token);
            when(sessionDomainService.requireActiveSession("session-001"))
                    .thenReturn(activeSession(null, Instant.now().plusSeconds(86400)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.refreshToken("raw-token", CLIENT_ID));

            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
            verify(refreshTokenDomainService, never()).rotate(any(), any());
        }

        @Test
        void refresh_insufficientSessionRemaining_isRejectedBeforeRotation() {
            stubClient();
            RefreshToken token = activeToken();
            when(refreshTokenDomainService.loadForRotation("raw-token", CLIENT_ID)).thenReturn(token);
            when(sessionDomainService.requireActiveSession("session-001"))
                    .thenReturn(activeSession("openid", Instant.now().plusSeconds(60)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.refreshToken("raw-token", CLIENT_ID));

            assertEquals(CiamErrorCode.SESSION_EXPIRED, ex.getErrorCode());
            verify(refreshTokenDomainService, never()).rotate(any(), any());
        }

        @Test
        void refresh_disabledUser_isRejected() {
            stubClient();
            RefreshToken token = activeToken();
            when(refreshTokenDomainService.loadForRotation("raw-token", CLIENT_ID)).thenReturn(token);
            when(sessionDomainService.requireActiveSession("session-001"))
                    .thenReturn(activeSession("openid", Instant.now().plusSeconds(86400)));
            when(userRepository.findByUserId("user-001"))
                    .thenReturn(Optional.of(User.builder()
                            .userId("user-001").userStatus(UserStatus.DISABLED.getCode()).build()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.refreshToken("raw-token", CLIENT_ID));

            assertEquals(CiamErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
            verify(refreshTokenDomainService, never()).rotate(any(), any());
        }

        @Test
        void refresh_replayPropagatesDedicatedException() {
            stubClient();
            when(refreshTokenDomainService.loadForRotation("raw-token", CLIENT_ID))
                    .thenThrow(new RefreshTokenReplayException("令牌已被使用，疑似重放攻击"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.refreshToken("raw-token", CLIENT_ID));

            assertTrue(ex instanceof RefreshTokenReplayException);
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }

    @Nested
    class LogoutTests {

        @Test
        void logout_delegatesToSessionDomainWithAuthenticatedContext() {
            service.logout("session-001", "user-001", CLIENT_ID);

            verify(sessionDomainService).logout("session-001", "user-001");
            verify(auditLogger).log(any());
        }
    }

    /**
     * 校验配置注入的允许清单被真实传递（防止"信任任意 X-Client-Id"）。
     */
    @Test
    void allowlistFromConfigurationIsPassedToClientPolicy() {
        stubClient();
        when(identityDomainService.findByTypeAndValue(any(), anyString())).thenReturn(Optional.empty());
        when(userDomainService.createUser(any(), any(), any(), any()))
                .thenReturn(User.builder().userId("user-005").userStatus(UserStatus.PENDING.getCode()).build());
        when(identityDomainService.bindIdentity(anyString(), any(), anyString(), any(), anyString()))
                .thenThrow(new DuplicateKeyException("stop-after-policy-check"));

        assertThrows(BusinessException.class, () -> service.loginByMobileCode(cmd()));

        verify(oauthClientDomainService).requireMobileClient(CLIENT_ID, List.of(CLIENT_ID));
    }
}
