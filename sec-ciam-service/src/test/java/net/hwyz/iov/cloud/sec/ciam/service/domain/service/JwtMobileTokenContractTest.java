package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 手机 Access Token 契约测试（SEC-CIAM-DSN-CR-003 US-003-3，TC-003-4/TC-003-11 CIAM 侧证据）。
 * <p>
 * 使用真实 RSA 密钥对签发与验签，断言 SGW {@code CiamTokenVerifier} 要求的必需声明：
 * {@code sub/client_id/client_type/device_id/session_id}、RS256 + kid、issuer、NumericDate 秒。
 */
class JwtMobileTokenContractTest {

    private static final String KID = "kid-contract-001";
    private static final String ISSUER = "https://account.openiov.top";

    private JwkDomainService jwkDomainService;
    private JwtTokenService jwtTokenService;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        publicKey = (RSAPublicKey) keyPair.getPublic();

        jwkDomainService = mock(JwkDomainService.class);
        when(jwkDomainService.getPrimaryPrivateKey()).thenReturn(privateKey);
        when(jwkDomainService.getPrimaryPublicKey()).thenReturn(publicKey);
        when(jwkDomainService.getPublicKeyByKeyId(anyString())).thenReturn(publicKey);
        when(jwkDomainService.getKeyId()).thenReturn(KID);

        jwtTokenService = new JwtTokenService(jwkDomainService);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> header(String token) throws Exception {
        String headerJson = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(headerJson, Map.class);
    }

    @Test
    void mobileToken_containsAllSgwRequiredClaims() throws Exception {
        String token = jwtTokenService.generateAccessToken(AccessTokenContext.builder()
                .userId("user-001")
                .deviceId("device-001")
                .clientId("ciam-mobile-app")
                .clientType("MOBILE")
                .scope("openid profile phone")
                .sessionId("session-001")
                .ttlSeconds(1800)
                .build());

        // 签名算法与密钥标识（SGW 本地验签前置条件）
        assertEquals("RS256", header(token).get("alg"));
        assertEquals(KID, header(token).get("kid"));

        Claims claims = Jwts.parser().verifyWith(publicKey).requireIssuer(ISSUER)
                .build().parseSignedClaims(token).getPayload();

        assertEquals("user-001", claims.getSubject());
        assertEquals("ciam-mobile-app", claims.get("client_id", String.class));
        assertEquals("MOBILE", claims.get("client_type", String.class));
        assertEquals("device-001", claims.get("device_id", String.class));
        assertEquals("session-001", claims.get("session_id", String.class));
        assertEquals("openid profile phone", claims.get("scope", String.class));
        assertEquals(ISSUER, claims.getIssuer());

        // NumericDate 秒（不得改为 ISO 字符串）
        long iatSeconds = claims.getIssuedAt().toInstant().getEpochSecond();
        long expSeconds = claims.getExpiration().toInstant().getEpochSecond();
        assertEquals(1800L, expSeconds - iatSeconds);
    }

    @Test
    void mobileToken_expIsCappedByAbsoluteSessionExpiry() throws Exception {
        Instant sessionExpiry = Instant.now().plusSeconds(600);

        String token = jwtTokenService.generateAccessToken(AccessTokenContext.builder()
                .userId("user-001")
                .deviceId("device-001")
                .clientId("ciam-mobile-app")
                .clientType("MOBILE")
                .scope("openid")
                .sessionId("session-001")
                .ttlSeconds(1800)
                .expireBound(sessionExpiry)
                .build());

        Claims claims = Jwts.parser().verifyWith(publicKey).requireIssuer(ISSUER)
                .build().parseSignedClaims(token).getPayload();

        assertTrue(claims.getExpiration().toInstant().getEpochSecond()
                        <= sessionExpiry.getEpochSecond(),
                "exp 必须被会话绝对到期边界收紧");
    }

    @Test
    void mobileToken_rejectedWhenSessionAlreadyExpired() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> jwtTokenService.generateAccessToken(AccessTokenContext.builder()
                        .userId("user-001")
                        .clientId("ciam-mobile-app")
                        .clientType("MOBILE")
                        .sessionId("session-001")
                        .ttlSeconds(1800)
                        .expireBound(Instant.now().minusSeconds(5))
                        .build()));
        assertEquals(CiamErrorCode.SESSION_EXPIRED, ex.getErrorCode());
    }

    @Test
    void legacyEntry_keepsOutOfScopeBehaviorWithoutClientType() {
        String token = jwtTokenService.generateAccessToken(
                "user-009", null, "client-legacy", "openid", null, 900);

        TokenClaims claims = jwtTokenService.validateAccessToken(token);

        assertEquals("user-009", claims.getSub());
        assertEquals("client-legacy", claims.getClientId());
        assertNull(claims.getClientType(), "范围外旧入口不得伪造终端类型");
        assertNull(claims.getSessionId());
    }

    @Test
    void mobileToken_roundTripsThroughValidator() {
        String token = jwtTokenService.generateAccessToken(AccessTokenContext.builder()
                .userId("user-001")
                .deviceId("device-001")
                .clientId("ciam-mobile-app")
                .clientType("MOBILE")
                .scope("openid profile")
                .sessionId("session-001")
                .ttlSeconds(1800)
                .build());

        TokenClaims claims = jwtTokenService.validateAccessToken(token);

        assertEquals("MOBILE", claims.getClientType());
        assertEquals("device-001", claims.getDeviceId());
        assertEquals("session-001", claims.getSessionId());
        assertEquals("openid profile", claims.getScope());
        assertEquals(ISSUER, claims.getIss());
    }
}
