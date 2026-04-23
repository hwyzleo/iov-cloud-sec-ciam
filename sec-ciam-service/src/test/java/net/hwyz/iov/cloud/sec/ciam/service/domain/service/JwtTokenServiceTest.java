package net.hwyz.iov.cloud.sec.ciam.service.domain.service;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import io.jsonwebtoken.Jwts;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenServiceTest {

    private static final String USER_ID = "user-jwt-001";
    private static final String CLIENT_ID = "client-app-001";
    private static final String SCOPE = "openid,profile";
    private static final String SESSION_ID = "session-001";
    private static final int TTL_SECONDS = 1800;

    private JwtTokenService service;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @Mock
    private JwkDomainService jwkDomainService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        when(jwkDomainService.getPrimaryPrivateKey()).thenReturn(privateKey);
        when(jwkDomainService.getPrimaryPublicKey()).thenReturn(publicKey);
        when(jwkDomainService.getKeyId()).thenReturn("test-key-id");
        when(jwkDomainService.getAllActiveKeys()).thenReturn(List.of());
        
        service = new JwtTokenService(jwkDomainService);
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
        void shouldGenerateRefreshTokenSuccessfully() {
            String token = service.generateRefreshToken(USER_ID, CLIENT_ID, SCOPE, SESSION_ID);
            assertNotNull(token);
        }
    }

    @Nested
    class ValidateTokenNegative {

        @Test
        void shouldThrowWhenTokenIsExpired() throws InterruptedException {
            String token = service.generateAccessToken(USER_ID, CLIENT_ID, SCOPE, SESSION_ID, 1);
            Thread.sleep(1500);

            assertThrows(BusinessException.class, () -> service.validateAccessToken(token));
        }

        @Test
        void shouldThrowWhenTokenIsTampered() {
            String validToken = service.generateAccessToken(USER_ID, CLIENT_ID, SCOPE, SESSION_ID, TTL_SECONDS);
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

            assertThrows(BusinessException.class, () -> service.validateAccessToken(tamperedToken));
        }

        @Test
        void shouldThrowWhenIssuerIsInvalid() {
            String token = Jwts.builder()
                    .subject(USER_ID)
                    .issuer("invalid-issuer")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(TTL_SECONDS)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            BusinessException ex = assertThrows(BusinessException.class, () -> service.validateAccessToken(token));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }

    @Nested
    class JwksGeneration {

        @Test
        @SuppressWarnings("unchecked")
        void shouldReturnJwksWithPublicKey() {
            Map<String, Object> jwks = service.getJwks();

            assertNotNull(jwks);
            assertTrue(jwks.containsKey("keys"));
            assertInstanceOf(List.class, jwks.get("keys"));
        }
    }
}
