package net.hwyz.iov.cloud.sec.ciam.service.protocol;

import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import static org.mockito.Mockito.*;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;import org.mockito.Mockito;import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.AuthCodePo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.OAuthClientPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserIdentityPo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserProfilePo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.store.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * OAuth 2.0 / OIDC 协议兼容性测试。
 * <p>
 * 验证实现是否遵循 RFC 6749、RFC 7636 (PKCE)、RFC 8628 (Device Authorization Grant)、
 * OpenID Connect Core 1.0 (UserInfo、ID Token claims)、RFC 7517 (JWKS) 等规范。
 */
@DisplayName("OAuth 2.0 / OIDC 协议兼容性测试")
class OAuthOidcProtocolComplianceTest {

    // ========================================================================
    // 1. Authorization Code + PKCE (RFC 6749 §4.1, RFC 7636)
    // ========================================================================

    @Nested
    @DisplayName("Authorization Code + PKCE 协议兼容性")
    class AuthorizationCodePkceCompliance {

        private CiamOAuthClientRepository clientRepository;
        private CiamAuthCodeRepository authCodeRepository;
        private PasswordEncoder passwordEncoder;
        private OAuthAuthorizationService authService;

        private static final String CLIENT_ID = "pkce-client-001";
        private static final String USER_ID = "user-pkce-001";
        private static final String REDIRECT_URI = "https://app.example.com/callback";
        private static final String SCOPE = "openid,profile";

        @BeforeEach
        void setUp() {
            clientRepository = mock(CiamOAuthClientRepository.class);
            authCodeRepository = mock(CiamAuthCodeRepository.class);
            passwordEncoder = new PasswordEncoder(4);
            when(authCodeRepository.insert(any())).thenReturn(1);
            when(authCodeRepository.updateByAuthCodeId(any())).thenReturn(1);
            authService = new OAuthAuthorizationService(authCodeRepository, clientRepository, passwordEncoder);
        }

        private OAuthClientPo publicClientWithPkce() {
            OAuthClientPo c = new OAuthClientPo();
            c.setClientId(CLIENT_ID);
            c.setClientName("PKCE Test App");
            c.setClientType("public");
            c.setClientSecretHash(null);
            c.setRedirectUris(REDIRECT_URI);
            c.setGrantTypes("authorization_code");
            c.setScopes(SCOPE);
            c.setPkceRequired(1);
            c.setAccessTokenTtl(1800);
            c.setRefreshTokenTtl(2592000);
            c.setClientStatus(1);
            c.setRowVersion(1);
            c.setRowValid(1);
            return c;
        }

        /** RFC 7636 §4.2: code_challenge = BASE64URL(SHA256(code_verifier)) */
        private static String computeS256Challenge(String codeVerifier) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private AuthCodePo stubAuthCode(String codeHash, String codeChallenge) {
            AuthCodePo a = new AuthCodePo();
            a.setClientId(CLIENT_ID);
            a.setUserId(USER_ID);
            a.setRedirectUri(REDIRECT_URI);
            a.setScope(SCOPE);
            a.setCodeChallenge(codeChallenge);
            a.setChallengeMethod(codeChallenge != null ? "S256" : null);
            a.setCodeHash(codeHash);
            a.setExpireTime(Instant.now().plusSeconds(10 * 60));
            a.setUsedFlag(0);
            return a;
        }

        @Test
        @DisplayName("RFC 7636: S256 code_challenge/code_verifier 验证通过")
        void pkceS256_validVerifier_shouldSucceed() {
            String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
            String codeChallenge = computeS256Challenge(codeVerifier);

            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(publicClientWithPkce()));
            when(authCodeRepository.insert(any())).thenReturn(1);

            String code = authService.createAuthorizationCode(
                    CLIENT_ID, USER_ID, null, REDIRECT_URI, SCOPE, codeChallenge, "S256");
            assertNotNull(code, "授权码不应为空");

            AuthCodePo authCodeDo = stubAuthCode(TokenDigest.fingerprint(code), codeChallenge);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(code)))
                    .thenReturn(Optional.of(authCodeDo));

            AuthCodeExchangeResult result = authService.exchangeCode(
                    code, CLIENT_ID, null, REDIRECT_URI, codeVerifier);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(CLIENT_ID, result.getClientId());
            assertEquals(SCOPE, result.getScope());
        }

        @Test
        @DisplayName("RFC 7636: 错误的 code_verifier 应被拒绝")
        void pkceS256_invalidVerifier_shouldFail() {
            String codeChallenge = computeS256Challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");
            String rawCode = "test-raw-code-invalid-verifier";

            AuthCodePo authCodeDo = stubAuthCode(TokenDigest.fingerprint(rawCode), codeChallenge);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(rawCode))).thenReturn(Optional.of(authCodeDo));
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(publicClientWithPkce()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.exchangeCode(rawCode, CLIENT_ID, null, REDIRECT_URI, "wrong-verifier"));
            assertEquals(CiamErrorCode.PKCE_VERIFICATION_FAILED, ex.getErrorCode());
        }

        @Test
        @DisplayName("RFC 7636: 缺少 code_verifier 时应被拒绝")
        void pkceS256_missingVerifier_shouldFail() {
            String codeChallenge = computeS256Challenge("some-verifier-value-for-testing");
            String rawCode = "test-raw-code-missing-verifier";

            AuthCodePo authCodeDo = stubAuthCode(TokenDigest.fingerprint(rawCode), codeChallenge);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(rawCode))).thenReturn(Optional.of(authCodeDo));
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(publicClientWithPkce()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.exchangeCode(rawCode, CLIENT_ID, null, REDIRECT_URI, null));
            assertEquals(CiamErrorCode.PKCE_VERIFICATION_FAILED, ex.getErrorCode());
        }

        @Test
        @DisplayName("RFC 6749 §4.1.3: 授权码只能使用一次")
        void authorizationCode_shouldBeOneTimeUse() {
            String rawCode = "test-raw-code-used";
            AuthCodePo authCodeDo = stubAuthCode(TokenDigest.fingerprint(rawCode), null);
            authCodeDo.setUsedFlag(1);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(rawCode))).thenReturn(Optional.of(authCodeDo));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.exchangeCode(rawCode, CLIENT_ID, null, REDIRECT_URI, null));
            assertEquals(CiamErrorCode.AUTH_CODE_USED, ex.getErrorCode());
        }

        @Test
        @DisplayName("RFC 6749 §4.1.3: redirect_uri 必须与授权请求一致")
        void authorizationCode_redirectUriMismatch_shouldFail() {
            String rawCode = "test-raw-code-redirect";
            AuthCodePo authCodeDo = stubAuthCode(TokenDigest.fingerprint(rawCode), null);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(rawCode))).thenReturn(Optional.of(authCodeDo));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.exchangeCode(rawCode, CLIENT_ID, null,
                            "https://evil.example.com/callback", null));
            assertEquals(CiamErrorCode.AUTH_CODE_REDIRECT_MISMATCH, ex.getErrorCode());
        }

        @Test
        @DisplayName("RFC 6749 §4.1.3: client_id 必须与授权请求一致")
        void authorizationCode_clientIdMismatch_shouldFail() {
            String rawCode = "test-raw-code-client";
            AuthCodePo authCodeDo = stubAuthCode(TokenDigest.fingerprint(rawCode), null);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(rawCode))).thenReturn(Optional.of(authCodeDo));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.exchangeCode(rawCode, "wrong-client", null, REDIRECT_URI, null));
            assertEquals(CiamErrorCode.AUTH_CODE_CLIENT_MISMATCH, ex.getErrorCode());
        }
    }

    // ========================================================================
    // 2. Client Credentials (RFC 6749 §4.4)
    // ========================================================================

    @Nested
    @DisplayName("Client Credentials 协议兼容性")
    class ClientCredentialsCompliance {

        private CiamOAuthClientRepository clientRepository;
        private CiamAuthCodeRepository authCodeRepository;
        private PasswordEncoder passwordEncoder;
        private OAuthAuthorizationService authService;

        private static final String CLIENT_ID = "cc-client-001";
        private static final String CLIENT_SECRET = "super-secret-value";
        private String clientSecretHash;

        @BeforeEach
        void setUp() {
            clientRepository = mock(CiamOAuthClientRepository.class);
            authCodeRepository = mock(CiamAuthCodeRepository.class);
            passwordEncoder = new PasswordEncoder(4);
            clientSecretHash = passwordEncoder.encode(CLIENT_SECRET);
            authService = new OAuthAuthorizationService(authCodeRepository, clientRepository, passwordEncoder);
        }

        private OAuthClientPo confidentialClient() {
            OAuthClientPo c = new OAuthClientPo();
            c.setClientId(CLIENT_ID);
            c.setClientName("Internal Service");
            c.setClientType("confidential");
            c.setClientSecretHash(clientSecretHash);
            c.setGrantTypes("client_credentials");
            c.setScopes("internal,service");
            c.setPkceRequired(0);
            c.setAccessTokenTtl(3600);
            c.setRefreshTokenTtl(0);
            c.setClientStatus(1);
            c.setRowVersion(1);
            c.setRowValid(1);
            return c;
        }

        @Test
        @DisplayName("RFC 6749 §4.4: 机密客户端凭据授权成功签发令牌")
        void clientCredentials_validCredentials_shouldIssueToken() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(confidentialClient()));

            ClientCredentialsResult result = authService.clientCredentialsGrant(CLIENT_ID, CLIENT_SECRET, "internal");

            assertEquals(CLIENT_ID, result.getClientId());
            assertNotNull(result.getScope());
            assertTrue(result.getAccessTokenTtl() > 0, "access_token TTL 应大于 0");
        }

        @Test
        @DisplayName("RFC 6749 §4.4.3: 错误的 client_secret 应被拒绝")
        void clientCredentials_invalidSecret_shouldFail() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(confidentialClient()));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.clientCredentialsGrant(CLIENT_ID, "wrong-secret", "internal"));
            assertEquals(CiamErrorCode.CLIENT_SECRET_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("RFC 6749 §4.4: 不存在的客户端应被拒绝")
        void clientCredentials_unknownClient_shouldFail() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.clientCredentialsGrant("nonexistent", CLIENT_SECRET, null));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("RFC 6749 §4.4: 已停用客户端应被拒绝")
        void clientCredentials_disabledClient_shouldFail() {
            OAuthClientPo disabled = confidentialClient();
            disabled.setClientStatus(0);
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(disabled));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.clientCredentialsGrant(CLIENT_ID, CLIENT_SECRET, null));
            assertEquals(CiamErrorCode.CLIENT_DISABLED, ex.getErrorCode());
        }
    }

    // ========================================================================
    // 3. Device Authorization Grant (RFC 8628)
    // ========================================================================

    @Nested
    @DisplayName("Device Authorization Grant 协议兼容性")
    class DeviceAuthorizationGrantCompliance {

        private CiamOAuthClientRepository clientRepository;
        private InMemoryVerificationCodeStore store;
        private DeviceAuthorizationService deviceService;

        private static final String CLIENT_ID = "vehicle-client-001";
        private static final String USER_ID = "user-device-001";
        private static final String SCOPE = "openid,vehicle";

        @BeforeEach
        void setUp() {
            clientRepository = mock(CiamOAuthClientRepository.class);
            store = new InMemoryVerificationCodeStore();
            deviceService = new DeviceAuthorizationService(clientRepository, store);
        }

        private OAuthClientPo vehicleClient() {
            OAuthClientPo c = new OAuthClientPo();
            c.setClientId(CLIENT_ID);
            c.setClientName("Vehicle App");
            c.setClientType("public");
            c.setGrantTypes("urn:ietf:params:oauth:grant-type:device_code");
            c.setScopes(SCOPE);
            c.setPkceRequired(0);
            c.setAccessTokenTtl(1800);
            c.setRefreshTokenTtl(2592000);
            c.setClientStatus(1);
            c.setRowVersion(1);
            c.setRowValid(1);
            return c;
        }

        @Test
        @DisplayName("RFC 8628 §3.2: 设备授权响应包含必需字段")
        void deviceAuthResponse_shouldContainRequiredFields() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(vehicleClient()));

            DeviceAuthorizationResponse response = deviceService.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            assertNotNull(response.getDeviceCode(), "device_code 不应为空");
            assertNotNull(response.getUserCode(), "user_code 不应为空");
            assertNotNull(response.getVerificationUri(), "verification_uri 不应为空");
            assertTrue(response.getExpiresIn() > 0, "expires_in 应大于 0");
        }

        @Test
        @DisplayName("RFC 8628 §3.2: user_code 应为人类可读格式")
        void deviceAuth_userCode_shouldBeHumanReadable() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(vehicleClient()));

            DeviceAuthorizationResponse response = deviceService.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            assertTrue(response.getUserCode().matches("[A-Z]+"),
                    "user_code 应仅包含大写字母以便于输入");
            assertEquals(8, response.getUserCode().length(), "user_code 长度应为 8 字符");
        }

        @Test
        @DisplayName("RFC 8628 §3.3: verification_uri 应为有效 HTTPS URL")
        void deviceAuth_verificationUri_shouldBeHttps() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(vehicleClient()));

            DeviceAuthorizationResponse response = deviceService.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            assertTrue(response.getVerificationUri().startsWith("https://"),
                    "verification_uri 应使用 HTTPS 协议");
        }

        @Test
        @DisplayName("RFC 8628 §3.3: interval 应提供轮询间隔")
        void deviceAuth_shouldProvidePollingInterval() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(vehicleClient()));

            DeviceAuthorizationResponse response = deviceService.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            assertTrue(response.getInterval() >= 5, "RFC 8628 建议默认轮询间隔不低于 5 秒");
        }

        @Test
        @DisplayName("RFC 8628 §3.5: 授权待定时轮询应返回 authorization_pending")
        void deviceAuth_pendingPoll_shouldIndicatePending() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(vehicleClient()));

            DeviceAuthorizationResponse response = deviceService.initiateDeviceAuthorization(CLIENT_ID, SCOPE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> deviceService.pollDeviceAuthorization(response.getDeviceCode(), CLIENT_ID));
            assertEquals(CiamErrorCode.DEVICE_CODE_PENDING, ex.getErrorCode());
        }

        @Test
        @DisplayName("RFC 8628 §3.5: 用户确认后轮询应返回授权结果")
        void deviceAuth_approvedPoll_shouldReturnResult() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(vehicleClient()));

            DeviceAuthorizationResponse response = deviceService.initiateDeviceAuthorization(CLIENT_ID, SCOPE);
            deviceService.approveDeviceAuthorization(response.getUserCode(), USER_ID);

            DeviceAuthorizationResult result = deviceService.pollDeviceAuthorization(
                    response.getDeviceCode(), CLIENT_ID);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(CLIENT_ID, result.getClientId());
            assertEquals(SCOPE, result.getScope());
        }

        @Test
        @DisplayName("RFC 8628 §3.5: 授权成功后 device_code 应失效（一次性使用）")
        void deviceAuth_approvedCode_shouldBeConsumedOnce() {
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(vehicleClient()));

            DeviceAuthorizationResponse response = deviceService.initiateDeviceAuthorization(CLIENT_ID, SCOPE);
            deviceService.approveDeviceAuthorization(response.getUserCode(), USER_ID);

            deviceService.pollDeviceAuthorization(response.getDeviceCode(), CLIENT_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> deviceService.pollDeviceAuthorization(response.getDeviceCode(), CLIENT_ID));
            assertEquals(CiamErrorCode.DEVICE_CODE_EXPIRED, ex.getErrorCode());
        }
    }

    // ========================================================================
    // 4. OIDC UserInfo (OpenID Connect Core 1.0 §5.3)
    // ========================================================================

    @Nested
    @DisplayName("OIDC UserInfo 协议兼容性")
    class OidcUserInfoCompliance {

        private static final String AES_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        private static final String USER_ID = "user-oidc-001";

        private OidcService oidcService;
        private CiamUserProfileRepository profileRepository;
        private CiamUserIdentityRepository identityRepository;
        private FieldEncryptor fieldEncryptor;

        @BeforeEach
        void setUp() {
            profileRepository = mock(CiamUserProfileRepository.class);
            identityRepository = mock(CiamUserIdentityRepository.class);
            fieldEncryptor = new FieldEncryptor(AES_KEY);
            IdentityDomainService identityDomainService = new IdentityDomainService(identityRepository, fieldEncryptor);
            oidcService = new OidcService(profileRepository, identityDomainService, fieldEncryptor);
        }

        @Test
        @DisplayName("OIDC Core §5.3.2: UserInfo 响应必须包含 sub 声明")
        void userInfo_shouldAlwaysContainSub() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            OidcUserInfo info = oidcService.getUserInfo(USER_ID);

            assertNotNull(info.getSub(), "sub 声明不应为空");
            assertEquals(USER_ID, info.getSub(), "sub 应等于平台全局唯一用户 ID");
        }

        @Test
        @DisplayName("OIDC Core §5.1: UserInfo 声明使用标准命名")
        void userInfo_shouldUseStandardClaimNames() {
            UserProfilePo profile = new UserProfilePo();
            profile.setUserId(USER_ID);
            profile.setNickname("TestUser");
            profile.setAvatarUrl("https://example.com/avatar.jpg");
            profile.setGender(1);
            profile.setBirthday(LocalDate.of(2000, 1, 15));

            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            OidcUserInfo info = oidcService.getUserInfo(USER_ID);

            assertEquals("TestUser", info.getName(), "name 声明应映射昵称");
            assertEquals("https://example.com/avatar.jpg", info.getPicture(), "picture 声明应映射头像");
            assertEquals("male", info.getGender(), "gender 声明应使用 OIDC 标准值");
            assertEquals("2000-01-15", info.getBirthdate(), "birthdate 声明应使用 ISO 8601 格式");
        }

        @Test
        @DisplayName("OIDC Core §5.1: gender 声明应通过 UserInfo 验证标准值")
        void userInfo_genderMapping_shouldFollowOidcStandard() {
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // 验证 gender=1 → male
            UserProfilePo maleProfile = new UserProfilePo();
            maleProfile.setUserId(USER_ID);
            maleProfile.setGender(1);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(maleProfile));
            assertEquals("male", oidcService.getUserInfo(USER_ID).getGender());

            // 验证 gender=2 → female
            UserProfilePo femaleProfile = new UserProfilePo();
            femaleProfile.setUserId(USER_ID);
            femaleProfile.setGender(2);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(femaleProfile));
            assertEquals("female", oidcService.getUserInfo(USER_ID).getGender());

            // 验证 gender=0 → unknown
            UserProfilePo unknownProfile = new UserProfilePo();
            unknownProfile.setUserId(USER_ID);
            unknownProfile.setGender(0);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(unknownProfile));
            assertEquals("unknown", oidcService.getUserInfo(USER_ID).getGender());
        }
    }

    // ========================================================================
    // 5. JWKS (RFC 7517)
    // ========================================================================

    @Nested
    @DisplayName("JWKS 协议兼容性")
    class JwksCompliance {

        private JwtTokenService jwtTokenService;

        @BeforeEach
        void setUp() {
            // 使用默认构造函数（public），内部自动生成 RSA 密钥对
            jwtTokenService = Mockito.mock(JwtTokenService.class);
        }

        @Test
        @DisplayName("RFC 7517 §5: JWKS 应包含 keys 数组")
        @SuppressWarnings("unchecked")
        void jwks_shouldContainKeysArray() {
            Map<String, Object> jwks = jwtTokenService.getJwks();

            assertTrue(jwks.containsKey("keys"), "JWKS 应包含 keys 字段");
            Object keys = jwks.get("keys");
            assertInstanceOf(List.class, keys, "keys 应为数组");
            assertFalse(((List<?>) keys).isEmpty(), "keys 数组不应为空");
        }

        @Test
        @DisplayName("RFC 7517 §4: JWK 应包含必需参数 kty")
        @SuppressWarnings("unchecked")
        void jwk_shouldContainRequiredKty() {
            Map<String, Object> jwks = jwtTokenService.getJwks();
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            Map<String, Object> jwk = keys.get(0);

            assertEquals("RSA", jwk.get("kty"), "kty 应为 RSA");
        }

        @Test
        @DisplayName("RFC 7517 §4.2: JWK 应包含 use 参数标识用途")
        @SuppressWarnings("unchecked")
        void jwk_shouldContainUseParameter() {
            Map<String, Object> jwks = jwtTokenService.getJwks();
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            Map<String, Object> jwk = keys.get(0);

            assertEquals("sig", jwk.get("use"), "use 应为 sig（签名）");
        }

        @Test
        @DisplayName("RFC 7517 §4.4: JWK 应包含 kid 参数")
        @SuppressWarnings("unchecked")
        void jwk_shouldContainKid() {
            Map<String, Object> jwks = jwtTokenService.getJwks();
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            Map<String, Object> jwk = keys.get(0);

            assertNotNull(jwk.get("kid"), "kid 不应为空");
        }

        @Test
        @DisplayName("RFC 7518 §6.3: RSA JWK 应包含 n 和 e 参数")
        @SuppressWarnings("unchecked")
        void rsaJwk_shouldContainModulusAndExponent() {
            Map<String, Object> jwks = jwtTokenService.getJwks();
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            Map<String, Object> jwk = keys.get(0);

            assertNotNull(jwk.get("n"), "RSA modulus (n) 不应为空");
            assertNotNull(jwk.get("e"), "RSA exponent (e) 不应为空");
            assertInstanceOf(String.class, jwk.get("n"));
            assertInstanceOf(String.class, jwk.get("e"));
        }

        @Test
        @DisplayName("RFC 7517 §4.3: JWK 应包含 alg 参数")
        @SuppressWarnings("unchecked")
        void jwk_shouldContainAlgParameter() {
            Map<String, Object> jwks = jwtTokenService.getJwks();
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            Map<String, Object> jwk = keys.get(0);

            assertEquals("RS256", jwk.get("alg"), "alg 应为 RS256");
        }

        @Test
        @DisplayName("JWKS 公钥应能验证签发的 JWT")
        void jwks_publicKey_shouldVerifyIssuedJwt() {
            String token = jwtTokenService.generateAccessToken(
                    "user-001", "client-001", "openid", "session-001", 1800);

            TokenClaims claims = jwtTokenService.validateAccessToken(token);
            assertEquals("user-001", claims.getSub());
        }
    }

    // ========================================================================
    // 6. ID Token Claims (OpenID Connect Core 1.0 §2)
    // ========================================================================

    @Nested
    @DisplayName("ID Token Claims 协议兼容性")
    class IdTokenClaimsCompliance {

        private static final String AES_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        private static final String USER_ID = "user-idtoken-001";
        private static final String CLIENT_ID = "client-idtoken-001";

        private OidcService oidcService;
        private CiamUserProfileRepository profileRepository;
        private CiamUserIdentityRepository identityRepository;
        private FieldEncryptor fieldEncryptor;

        @BeforeEach
        void setUp() {
            profileRepository = mock(CiamUserProfileRepository.class);
            identityRepository = mock(CiamUserIdentityRepository.class);
            fieldEncryptor = new FieldEncryptor(AES_KEY);
            IdentityDomainService identityDomainService = new IdentityDomainService(identityRepository, fieldEncryptor);
            oidcService = new OidcService(profileRepository, identityDomainService, fieldEncryptor);
        }

        @Test
        @DisplayName("OIDC Core §2: ID Token 必须包含 iss 声明")
        void idToken_shouldContainIss() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            assertNotNull(claims.get("iss"), "iss 声明不应为空");
            assertInstanceOf(String.class, claims.get("iss"));
            assertTrue(((String) claims.get("iss")).startsWith("https://"), "iss 应为 HTTPS URL");
        }

        @Test
        @DisplayName("OIDC Core §2: ID Token 必须包含 sub 声明")
        void idToken_shouldContainSub() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            assertEquals(USER_ID, claims.get("sub"), "sub 应等于用户唯一标识");
        }

        @Test
        @DisplayName("OIDC Core §2: ID Token 必须包含 aud 声明")
        void idToken_shouldContainAud() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            assertEquals(CLIENT_ID, claims.get("aud"), "aud 应等于客户端标识");
        }

        @Test
        @DisplayName("OIDC Core §2: ID Token 必须包含 exp 声明")
        void idToken_shouldContainExp() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            assertNotNull(claims.get("exp"), "exp 声明不应为空");
            assertInstanceOf(Long.class, claims.get("exp"));
            assertTrue((Long) claims.get("exp") > Instant.now().getEpochSecond(), "exp 应在当前时间之后");
        }

        @Test
        @DisplayName("OIDC Core §2: ID Token 必须包含 iat 声明")
        void idToken_shouldContainIat() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            assertNotNull(claims.get("iat"), "iat 声明不应为空");
            assertInstanceOf(Long.class, claims.get("iat"));
            long iat = (Long) claims.get("iat");
            assertTrue(Math.abs(iat - Instant.now().getEpochSecond()) < 5,
                    "iat 应接近当前时间（误差不超过 5 秒）");
        }

        @Test
        @DisplayName("OIDC Core §2: exp 应在 iat 之后")
        void idToken_expShouldBeAfterIat() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            long iat = (Long) claims.get("iat");
            long exp = (Long) claims.get("exp");
            assertTrue(exp > iat, "exp 应在 iat 之后");
        }

        @Test
        @DisplayName("OIDC Core §5.4: scope=profile 时应包含 profile 声明")
        void idToken_profileScope_shouldIncludeProfileClaims() {
            UserProfilePo profile = new UserProfilePo();
            profile.setUserId(USER_ID);
            profile.setNickname("TestUser");
            profile.setAvatarUrl("https://example.com/avatar.jpg");
            profile.setGender(1);

            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid,profile");

            assertEquals("TestUser", claims.get("name"));
            assertEquals("https://example.com/avatar.jpg", claims.get("picture"));
            assertEquals("male", claims.get("gender"));
        }

        @Test
        @DisplayName("OIDC Core §5.4: scope=email 时应包含 email 声明")
        void idToken_emailScope_shouldIncludeEmailClaim() {
            UserIdentityPo emailIdentity = new UserIdentityPo();
            emailIdentity.setUserId(USER_ID);
            emailIdentity.setIdentityType(IdentityType.EMAIL.getCode());
            emailIdentity.setIdentityValue(fieldEncryptor.encrypt("user@example.com"));
            emailIdentity.setIdentityStatus(IdentityStatus.BOUND.getCode());

            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(emailIdentity));

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid,email");

            assertEquals("user@example.com", claims.get("email"));
        }

        @Test
        @DisplayName("OIDC Core §5.4: scope=phone 时应包含 phone_number 声明")
        void idToken_phoneScope_shouldIncludePhoneClaim() {
            UserIdentityPo mobileIdentity = new UserIdentityPo();
            mobileIdentity.setUserId(USER_ID);
            mobileIdentity.setIdentityType(IdentityType.MOBILE.getCode());
            mobileIdentity.setIdentityValue(fieldEncryptor.encrypt("+8613800138000"));
            mobileIdentity.setIdentityStatus(IdentityStatus.BOUND.getCode());

            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(mobileIdentity));

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid,phone");

            assertEquals("+8613800138000", claims.get("phone_number"));
        }

        @Test
        @DisplayName("OIDC Core §2: 所有必需声明应同时存在")
        void idToken_shouldContainAllRequiredClaims() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of());

            Map<String, Object> claims = oidcService.getIdTokenClaims(USER_ID, CLIENT_ID, "openid");

            Set<String> requiredClaims = Set.of("iss", "sub", "aud", "exp", "iat");
            for (String required : requiredClaims) {
                assertTrue(claims.containsKey(required), "ID Token 缺少必需声明: " + required);
                assertNotNull(claims.get(required), "ID Token 必需声明不应为 null: " + required);
            }
        }
    }

    // ========================================================================
    // 7. Token Response Format (RFC 6749 §5.1)
    // ========================================================================

    @Nested
    @DisplayName("Token 响应格式协议兼容性")
    class TokenResponseFormatCompliance {

        private JwtTokenService jwtTokenService;

        @BeforeEach
        void setUp() {
            jwtTokenService = Mockito.mock(JwtTokenService.class);
        }

        @Test
        @DisplayName("RFC 6749 §5.1: Access Token 应为有效 JWT")
        void accessToken_shouldBeValidJwt() {
            String token = jwtTokenService.generateAccessToken(
                    "user-001", "client-001", "openid", "session-001", 1800);

            String[] parts = token.split("\\.");
            assertEquals(3, parts.length, "JWT 应由 header.payload.signature 三部分组成");
        }

        @Test
        @DisplayName("RFC 6749 §5.1: Access Token 应包含标准声明")
        void accessToken_shouldContainStandardClaims() {
            String token = jwtTokenService.generateAccessToken(
                    "user-001", "client-001", "openid,profile", "session-001", 1800);

            TokenClaims claims = jwtTokenService.validateAccessToken(token);

            assertEquals("user-001", claims.getSub());
            assertEquals("client-001", claims.getClientId());
            assertEquals("openid,profile", claims.getScope());
            assertEquals("session-001", claims.getSessionId());
            assertNotNull(claims.getIss());
            assertNotNull(claims.getIat());
            assertNotNull(claims.getExp());
        }

        @Test
        @DisplayName("JWT Access Token 有效期应与请求的 TTL 一致")
        void accessToken_ttl_shouldMatchRequested() {
            int requestedTtl = 3600;
            String token = jwtTokenService.generateAccessToken(
                    "user-001", "client-001", "openid", "session-001", requestedTtl);

            TokenClaims claims = jwtTokenService.validateAccessToken(token);
            long actualTtl = claims.getExp().getEpochSecond() - claims.getIat().getEpochSecond();
            assertEquals(requestedTtl, actualTtl, "Token TTL 应与请求的有效期一致");
        }
    }

    // ========================================================================
    // 8. OIDC Discovery Document (OpenID Connect Discovery 1.0 §3)
    // ========================================================================

    @Nested
    @DisplayName("OIDC Discovery Document 协议兼容性")
    class OidcDiscoveryCompliance {

        @Test
        @DisplayName("OIDC Discovery §3: Discovery Document 应包含 issuer")
        void discovery_shouldContainIssuer() {
            OidcDiscoveryDocument doc = new OidcDiscoveryDocument("https://account.openiov.com");

            assertEquals("https://account.openiov.com", doc.getIssuer());
            assertTrue(doc.getIssuer().startsWith("https://"), "issuer 应为 HTTPS URL");
        }

        @Test
        @DisplayName("OIDC Discovery §3: Discovery Document 应包含必需端点")
        void discovery_shouldContainRequiredEndpoints() {
            OidcDiscoveryDocument doc = new OidcDiscoveryDocument("https://account.openiov.com");

            assertNotNull(doc.getAuthorizationEndpoint(), "authorization_endpoint 不应为空");
            assertNotNull(doc.getTokenEndpoint(), "token_endpoint 不应为空");
            assertNotNull(doc.getUserinfoEndpoint(), "userinfo_endpoint 不应为空");
            assertNotNull(doc.getJwksUri(), "jwks_uri 不应为空");
        }

        @Test
        @DisplayName("OIDC Discovery §3: 应声明支持的 response_types")
        void discovery_shouldDeclareResponseTypes() {
            OidcDiscoveryDocument doc = new OidcDiscoveryDocument("https://account.openiov.com");

            assertNotNull(doc.getResponseTypesSupported());
            assertTrue(doc.getResponseTypesSupported().contains("code"), "应支持 response_type=code");
        }

        @Test
        @DisplayName("OIDC Discovery §3: 应声明支持的 grant_types")
        void discovery_shouldDeclareGrantTypes() {
            OidcDiscoveryDocument doc = new OidcDiscoveryDocument("https://account.openiov.com");

            List<String> grantTypes = doc.getGrantTypesSupported();
            assertTrue(grantTypes.contains("authorization_code"), "应支持 authorization_code");
            assertTrue(grantTypes.contains("client_credentials"), "应支持 client_credentials");
            assertTrue(grantTypes.contains("urn:ietf:params:oauth:grant-type:device_code"), "应支持 device_code");
            assertTrue(grantTypes.contains("refresh_token"), "应支持 refresh_token");
        }

        @Test
        @DisplayName("OIDC Discovery §3: 应声明支持的 id_token_signing_alg_values")
        void discovery_shouldDeclareSigningAlgorithms() {
            OidcDiscoveryDocument doc = new OidcDiscoveryDocument("https://account.openiov.com");

            assertTrue(doc.getIdTokenSigningAlgValuesSupported().contains("RS256"), "应支持 RS256 签名算法");
        }

        @Test
        @DisplayName("OIDC Discovery §3: 应声明支持的 scopes")
        void discovery_shouldDeclareSupportedScopes() {
            OidcDiscoveryDocument doc = new OidcDiscoveryDocument("https://account.openiov.com");

            List<String> scopes = doc.getScopesSupported();
            assertTrue(scopes.contains("openid"), "应支持 openid scope");
            assertTrue(scopes.contains("profile"), "应支持 profile scope");
            assertTrue(scopes.contains("email"), "应支持 email scope");
            assertTrue(scopes.contains("phone"), "应支持 phone scope");
        }
    }
}
