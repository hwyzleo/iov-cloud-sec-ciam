package net.hwyz.iov.cloud.sec.ciam.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.sec.ciam.common.security.TokenDigest;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamAuthCodeRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamOAuthClientRepository;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamAuthCodeDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OAuthAuthorizationServiceTest {

    private CiamAuthCodeRepository authCodeRepository;
    private CiamOAuthClientRepository clientRepository;
    private PasswordEncoder passwordEncoder;
    private OAuthAuthorizationService service;

    private static final String CLIENT_ID = "test-client-001";
    private static final String USER_ID = "user-001";
    private static final String SESSION_ID = "session-001";
    private static final String REDIRECT_URI = "https://example.com/callback";
    private static final String SCOPE = "openid,profile";
    private static final String CLIENT_SECRET = "super-secret";

    @BeforeEach
    void setUp() {
        authCodeRepository = mock(CiamAuthCodeRepository.class);
        clientRepository = mock(CiamOAuthClientRepository.class);
        passwordEncoder = new PasswordEncoder(4);
        when(authCodeRepository.insert(any())).thenReturn(1);
        when(authCodeRepository.updateByAuthCodeId(any())).thenReturn(1);
        service = new OAuthAuthorizationService(authCodeRepository, clientRepository, passwordEncoder);
    }

    private CiamOAuthClientDo stubPublicClient(boolean pkceRequired) {
        CiamOAuthClientDo client = new CiamOAuthClientDo();
        client.setClientId(CLIENT_ID);
        client.setClientName("Test App");
        client.setClientType("public");
        client.setClientSecretHash(null);
        client.setRedirectUris("https://example.com/callback,https://example.com/cb2");
        client.setGrantTypes("authorization_code");
        client.setScopes("openid,profile");
        client.setPkceRequired(pkceRequired ? 1 : 0);
        client.setAccessTokenTtl(1800);
        client.setRefreshTokenTtl(2592000);
        client.setClientStatus(1);
        client.setRowVersion(1);
        client.setRowValid(1);
        return client;
    }

    private CiamOAuthClientDo stubConfidentialClient(boolean pkceRequired) {
        CiamOAuthClientDo client = stubPublicClient(pkceRequired);
        client.setClientType("confidential");
        client.setClientSecretHash(passwordEncoder.encode(CLIENT_SECRET));
        return client;
    }

    private CiamAuthCodeDo stubAuthCode(String rawCode, String clientId, String redirectUri,
                                        String codeChallenge, String challengeMethod,
                                        Instant expireTime, int usedFlag) {
        CiamAuthCodeDo authCode = new CiamAuthCodeDo();
        authCode.setAuthCodeId("ac-001");
        authCode.setClientId(clientId);
        authCode.setUserId(USER_ID);
        authCode.setSessionId(SESSION_ID);
        authCode.setCodeHash(TokenDigest.fingerprint(rawCode));
        authCode.setRedirectUri(redirectUri);
        authCode.setScope(SCOPE);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setChallengeMethod(challengeMethod);
        authCode.setExpireTime(expireTime);
        authCode.setUsedFlag(usedFlag);
        authCode.setRowVersion(1);
        authCode.setRowValid(1);
        return authCode;
    }

    /** Helper: compute PKCE S256 code_challenge from a code_verifier */
    private static String computeS256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- createAuthorizationCode ----

    @Nested
    class CreateAuthorizationCodeTests {

        @Test
        void createCode_successfully() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubPublicClient(false)));

            String code = service.createAuthorizationCode(
                    CLIENT_ID, USER_ID, SESSION_ID, REDIRECT_URI, SCOPE, null, null);

            assertNotNull(code);
            assertFalse(code.isBlank());
            verify(authCodeRepository).insert(argThat(entity -> {
                assertEquals(CLIENT_ID, entity.getClientId());
                assertEquals(USER_ID, entity.getUserId());
                assertEquals(SESSION_ID, entity.getSessionId());
                assertEquals(REDIRECT_URI, entity.getRedirectUri());
                assertEquals(SCOPE, entity.getScope());
                assertNotNull(entity.getCodeHash());
                assertEquals(0, entity.getUsedFlag());
                assertNull(entity.getCodeChallenge());
                return true;
            }));
        }

        @Test
        void createCode_withPkceChallenge() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubPublicClient(true)));

            String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
            String codeChallenge = computeS256Challenge(codeVerifier);

            String code = service.createAuthorizationCode(
                    CLIENT_ID, USER_ID, SESSION_ID, REDIRECT_URI, SCOPE, codeChallenge, "S256");

            assertNotNull(code);
            verify(authCodeRepository).insert(argThat(entity -> {
                assertEquals(codeChallenge, entity.getCodeChallenge());
                assertEquals("S256", entity.getChallengeMethod());
                return true;
            }));
        }

        @Test
        void createCode_failsWhenClientNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createAuthorizationCode(
                            "nonexistent", USER_ID, SESSION_ID, REDIRECT_URI, SCOPE, null, null));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void createCode_failsWhenClientDisabled() {
            CiamOAuthClientDo client = stubPublicClient(false);
            client.setClientStatus(0);
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createAuthorizationCode(
                            CLIENT_ID, USER_ID, SESSION_ID, REDIRECT_URI, SCOPE, null, null));
            assertEquals(CiamErrorCode.CLIENT_DISABLED, ex.getErrorCode());
        }

        @Test
        void createCode_failsWhenRedirectUriNotRegistered() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubPublicClient(false)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createAuthorizationCode(
                            CLIENT_ID, USER_ID, SESSION_ID, "https://evil.com/cb", SCOPE, null, null));
            assertEquals(CiamErrorCode.REDIRECT_URI_INVALID, ex.getErrorCode());
        }

        @Test
        void createCode_failsWhenPkceRequiredButMissing() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubPublicClient(true)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createAuthorizationCode(
                            CLIENT_ID, USER_ID, SESSION_ID, REDIRECT_URI, SCOPE, null, null));
            assertEquals(CiamErrorCode.PKCE_CHALLENGE_REQUIRED, ex.getErrorCode());
        }
    }

    // ---- exchangeCode ----

    @Nested
    class ExchangeCodeTests {

        private static final String RAW_CODE = "test-authorization-code-value";

        @Test
        void exchangeCode_successfully() {
            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    null, null, Instant.now().plusSeconds(5 * 60), 0);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubPublicClient(false)));

            AuthCodeExchangeResult result = service.exchangeCode(
                    RAW_CODE, CLIENT_ID, null, REDIRECT_URI, null);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(SESSION_ID, result.getSessionId());
            assertEquals(SCOPE, result.getScope());
            assertEquals(CLIENT_ID, result.getClientId());
            verify(authCodeRepository).updateByAuthCodeId(argThat(entity -> {
                assertEquals(1, entity.getUsedFlag());
                assertNotNull(entity.getUsedTime());
                return true;
            }));
        }

        @Test
        void exchangeCode_withPkceVerification() {
            String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
            String codeChallenge = computeS256Challenge(codeVerifier);

            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    codeChallenge, "S256", Instant.now().plusSeconds(5 * 60), 0);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubPublicClient(true)));

            AuthCodeExchangeResult result = service.exchangeCode(
                    RAW_CODE, CLIENT_ID, null, REDIRECT_URI, codeVerifier);

            assertEquals(USER_ID, result.getUserId());
            assertEquals(SESSION_ID, result.getSessionId());
        }

        @Test
        void exchangeCode_failsWhenExpired() {
            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    null, null, Instant.now().minusSeconds(1 * 60), 0);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.exchangeCode(RAW_CODE, CLIENT_ID, null, REDIRECT_URI, null));
            assertEquals(CiamErrorCode.AUTH_CODE_EXPIRED, ex.getErrorCode());
        }

        @Test
        void exchangeCode_failsWhenAlreadyUsed() {
            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    null, null, Instant.now().plusSeconds(5 * 60), 1);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.exchangeCode(RAW_CODE, CLIENT_ID, null, REDIRECT_URI, null));
            assertEquals(CiamErrorCode.AUTH_CODE_USED, ex.getErrorCode());
        }

        @Test
        void exchangeCode_failsWhenWrongRedirectUri() {
            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    null, null, Instant.now().plusSeconds(5 * 60), 0);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.exchangeCode(RAW_CODE, CLIENT_ID, null, "https://wrong.com/cb", null));
            assertEquals(CiamErrorCode.AUTH_CODE_REDIRECT_MISMATCH, ex.getErrorCode());
        }

        @Test
        void exchangeCode_failsWhenWrongClientId() {
            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    null, null, Instant.now().plusSeconds(5 * 60), 0);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.exchangeCode(RAW_CODE, "wrong-client", null, REDIRECT_URI, null));
            assertEquals(CiamErrorCode.AUTH_CODE_CLIENT_MISMATCH, ex.getErrorCode());
        }

        @Test
        void exchangeCode_failsWhenInvalidPkceVerifier() {
            String codeVerifier = "correct-verifier-value";
            String codeChallenge = computeS256Challenge(codeVerifier);

            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    codeChallenge, "S256", Instant.now().plusSeconds(5 * 60), 0);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.exchangeCode(RAW_CODE, CLIENT_ID, null, REDIRECT_URI, "wrong-verifier"));
            assertEquals(CiamErrorCode.PKCE_VERIFICATION_FAILED, ex.getErrorCode());
        }

        @Test
        void exchangeCode_failsWhenCodeNotFound() {
            when(authCodeRepository.findByCodeHash(any())).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.exchangeCode("nonexistent-code", CLIENT_ID, null, REDIRECT_URI, null));
            assertEquals(CiamErrorCode.AUTH_CODE_INVALID, ex.getErrorCode());
        }

        @Test
        void exchangeCode_confidentialClient_failsWithWrongSecret() {
            CiamAuthCodeDo authCode = stubAuthCode(RAW_CODE, CLIENT_ID, REDIRECT_URI,
                    null, null, Instant.now().plusSeconds(5 * 60), 0);
            when(authCodeRepository.findByCodeHash(TokenDigest.fingerprint(RAW_CODE)))
                    .thenReturn(Optional.of(authCode));
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubConfidentialClient(false)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.exchangeCode(RAW_CODE, CLIENT_ID, "wrong-secret", REDIRECT_URI, null));
            assertEquals(CiamErrorCode.CLIENT_SECRET_INVALID, ex.getErrorCode());
        }
    }

    // ---- clientCredentialsGrant ----

    @Nested
    class ClientCredentialsGrantTests {

        @Test
        void clientCredentialsGrant_successfully() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubConfidentialClient(false)));

            ClientCredentialsResult result = service.clientCredentialsGrant(
                    CLIENT_ID, CLIENT_SECRET, "openid,profile");

            assertEquals(CLIENT_ID, result.getClientId());
            assertEquals("openid,profile", result.getScope());
            assertEquals(1800, result.getAccessTokenTtl());
        }

        @Test
        void clientCredentialsGrant_withNullScope_returnsAllAllowedScopes() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubConfidentialClient(false)));

            ClientCredentialsResult result = service.clientCredentialsGrant(
                    CLIENT_ID, CLIENT_SECRET, null);

            assertEquals("openid,profile", result.getScope());
        }

        @Test
        void clientCredentialsGrant_failsWithInvalidSecret() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubConfidentialClient(false)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.clientCredentialsGrant(CLIENT_ID, "wrong-secret", SCOPE));
            assertEquals(CiamErrorCode.CLIENT_SECRET_INVALID, ex.getErrorCode());
        }

        @Test
        void clientCredentialsGrant_failsWithDisabledClient() {
            CiamOAuthClientDo client = stubConfidentialClient(false);
            client.setClientStatus(0);
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.clientCredentialsGrant(CLIENT_ID, CLIENT_SECRET, SCOPE));
            assertEquals(CiamErrorCode.CLIENT_DISABLED, ex.getErrorCode());
        }

        @Test
        void clientCredentialsGrant_failsWithPublicClient() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubPublicClient(false)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.clientCredentialsGrant(CLIENT_ID, "any-secret", SCOPE));
            assertEquals(CiamErrorCode.CLIENT_TYPE_NOT_SUPPORTED, ex.getErrorCode());
        }

        @Test
        void clientCredentialsGrant_failsWithScopeExceeded() {
            when(clientRepository.findByClientId(CLIENT_ID))
                    .thenReturn(Optional.of(stubConfidentialClient(false)));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.clientCredentialsGrant(CLIENT_ID, CLIENT_SECRET, "openid,admin"));
            assertEquals(CiamErrorCode.SCOPE_EXCEEDED, ex.getErrorCode());
        }

        @Test
        void clientCredentialsGrant_withInternalClient_successfully() {
            CiamOAuthClientDo client = stubConfidentialClient(false);
            client.setClientType("internal");
            when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));

            ClientCredentialsResult result = service.clientCredentialsGrant(
                    CLIENT_ID, CLIENT_SECRET, "openid");

            assertEquals(CLIENT_ID, result.getClientId());
            assertEquals("openid", result.getScope());
        }

        @Test
        void clientCredentialsGrant_failsWhenClientNotFound() {
            when(clientRepository.findByClientId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.clientCredentialsGrant("nonexistent", CLIENT_SECRET, SCOPE));
            assertEquals(CiamErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ---- PKCE S256 verification ----

    @Nested
    class PkceVerificationTests {

        @Test
        void verifyPkceS256_validPair() {
            String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
            String codeChallenge = computeS256Challenge(codeVerifier);

            assertTrue(OAuthAuthorizationService.verifyPkceS256(codeVerifier, codeChallenge));
        }

        @Test
        void verifyPkceS256_invalidPair() {
            String codeChallenge = computeS256Challenge("correct-verifier");

            assertFalse(OAuthAuthorizationService.verifyPkceS256("wrong-verifier", codeChallenge));
        }
    }
}
