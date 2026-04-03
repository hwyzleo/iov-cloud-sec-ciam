package net.hwyz.iov.cloud.sec.ciam.domain.service;

import io.jsonwebtoken.Jwts;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private static final String USER_ID = "user-jwt-001";
    private static final String CLIENT_ID = "client-app-001";
    private static final String SCOPE = "openid,profile";
    private static final String SESSION_ID = "session-001";
    private static final int TTL_SECONDS = 1800;

    private JwtTokenService service;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        service = new JwtTokenService(publicKey, privateKey);
    }

    @Nested
    class GenerateAndValidateRoundTrip {

        @Test
        void shouldGenerateAndValidateTokenSuccessfully() {
            String token = service.generateAccessToken(USER_ID, CLIENT_ID, SCOPE, SESSION_ID, TTL_SECONDS);
            assertNotNull(token);

            TokenClaims claims = service.validateAccessToken(token);
            assertEquals(USER_ID, claims.getSub());
            assertEquals(CLIENT_ID, claims.getClientId());
            assertEquals(SCOPE, claims.getScope());
            assertEquals(SESSION_ID, claims.getSessionId());
            assertEquals(JwtTokenService.ISSUER, claims.getIss());
            assertNotNull(claims.getIat());
            assertNotNull(claims.getExp());
            assertTrue(claims.getExp().isAfter(claims.getIat()));
        }

        @Test
        void tokenShouldContainExpectedClaims() {
            String token = service.generateAccessToken(USER_ID, CLIENT_ID, SCOPE, SESSION_ID, 3600);
            TokenClaims claims = service.validateAccessToken(token);

            // exp should be approximately iat + ttl
            long diff = claims.getExp().getEpochSecond() - claims.getIat().getEpochSecond();
            assertEquals(3600, diff);
        }
    }

    @Nested
    class ValidateExpiredToken {

        @Test
        void shouldRejectExpiredToken() {
            // Build a token that is already expired using jjwt directly
            Instant past = Instant.now().minusSeconds(3600);
            String expiredToken = Jwts.builder()
                    .header().keyId(JwtTokenService.KEY_ID).and()
                    .subject(USER_ID)
                    .issuer(JwtTokenService.ISSUER)
                    .issuedAt(Date.from(past.minusSeconds(1800)))
                    .expiration(Date.from(past))
                    .claim("client_id", CLIENT_ID)
                    .claim("scope", SCOPE)
                    .claim("session_id", SESSION_ID)
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.validateAccessToken(expiredToken));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }

    @Nested
    class ValidateTamperedToken {

        @Test
        void shouldRejectTokenSignedWithDifferentKey() throws Exception {
            // Generate a different key pair
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair otherKeyPair = generator.generateKeyPair();
            RSAPrivateKey otherPrivateKey = (RSAPrivateKey) otherKeyPair.getPrivate();

            String tamperedToken = Jwts.builder()
                    .header().keyId(JwtTokenService.KEY_ID).and()
                    .subject(USER_ID)
                    .issuer(JwtTokenService.ISSUER)
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(1800)))
                    .claim("client_id", CLIENT_ID)
                    .claim("scope", SCOPE)
                    .claim("session_id", SESSION_ID)
                    .signWith(otherPrivateKey, Jwts.SIG.RS256)
                    .compact();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.validateAccessToken(tamperedToken));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        void shouldRejectMalformedToken() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.validateAccessToken("not.a.valid.jwt"));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }

    @Nested
    class GetJwks {

        @Test
        @SuppressWarnings("unchecked")
        void shouldReturnJwksWithPublicKey() {
            Map<String, Object> jwks = service.getJwks();
            assertNotNull(jwks);
            assertTrue(jwks.containsKey("keys"));

            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            assertEquals(1, keys.size());

            Map<String, Object> jwk = keys.get(0);
            assertEquals("RSA", jwk.get("kty"));
            assertEquals("sig", jwk.get("use"));
            assertEquals("RS256", jwk.get("alg"));
            assertEquals(JwtTokenService.KEY_ID, jwk.get("kid"));
            assertNotNull(jwk.get("n"));
            assertNotNull(jwk.get("e"));
        }
    }
}
