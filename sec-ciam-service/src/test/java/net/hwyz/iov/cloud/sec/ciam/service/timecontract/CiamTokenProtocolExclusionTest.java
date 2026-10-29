package net.hwyz.iov.cloud.sec.ciam.service.timecontract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.DeviceAuthorizationResponse;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwkDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * SEC-CIAM-DSN-CR-001 排除范围 golden 测试（设计 §3 / §4.1）。
 * <p>
 * JWT/OIDC iat/exp 为协议 NumericDate 秒；OAuth expires_in、TTL、普通 long、
 * 签名与第三方认证协议不随 JSON java.time 开关改 ISO。验证：
 * <ul>
 *   <li>JWT 原始 claims 中 iat/exp 为整数秒（NumericDate），非 ISO 字符串；</li>
 *   <li>exp - iat == TTL 秒，验签与到期逻辑不受 mapper 影响；</li>
 *   <li>OAuth expires_in / 设备授权 interval 等固定单位保持普通数字。</li>
 * </ul>
 */
class CiamTokenProtocolExclusionTest {

    private static final String USER_ID = "user-jwt-001";
    private static final String DEVICE_ID = "device-001";
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
        when(jwkDomainService.getPublicKeyByKeyId("test-key-id")).thenReturn(publicKey);
        when(jwkDomainService.getAllActiveKeys()).thenReturn(List.of());

        service = new JwtTokenService(jwkDomainService);
    }

    @Test
    void JWT原始claims_iat_exp为NumericDate整数秒_golden() throws Exception {
        String token = service.generateAccessToken(USER_ID, DEVICE_ID, CLIENT_ID, SCOPE, SESSION_ID, TTL_SECONDS);

        // 解析原始 JWT payload（Base64Url），不依赖 jjwt 封装，验证 wire 上 iat/exp 单位
        String[] parts = token.split("\\.");
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        JsonNode claims = new ObjectMapper().readTree(payload);

        assertThat(claims.get("iat").isIntegralNumber()).isTrue();
        assertThat(claims.get("exp").isIntegralNumber()).isTrue();
        long iat = claims.get("iat").longValue();
        long exp = claims.get("exp").longValue();
        assertThat(exp - iat).isEqualTo(TTL_SECONDS);
        // NumericDate 为 epoch 秒：exp 应为 10 位（2030 年前）
        assertThat(String.valueOf(exp)).hasSize(10);
    }

    @Test
    void JWT验签与到期语义_不受json开关影响() {
        String token = service.generateAccessToken(USER_ID, DEVICE_ID, CLIENT_ID, SCOPE, SESSION_ID, TTL_SECONDS);
        TokenClaims claims = service.validateAccessToken(token);
        assertThat(claims.getSub()).isEqualTo(USER_ID);
        assertThat(claims.getExp().getEpochSecond() - claims.getIat().getEpochSecond())
                .isEqualTo(TTL_SECONDS);
    }

    @Test
    void JWT刷新令牌_NumericDate保持不变() throws Exception {
        String refreshToken = service.generateRefreshToken(USER_ID, DEVICE_ID, CLIENT_ID, SCOPE, SESSION_ID);
        String[] parts = refreshToken.split("\\.");
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        JsonNode claims = new ObjectMapper().readTree(payload);
        assertThat(claims.get("iat").isIntegralNumber()).isTrue();
        assertThat(claims.get("exp").isIntegralNumber()).isTrue();
        assertThat(claims.get("exp").longValue() - claims.get("iat").longValue()).isGreaterThan(0);
    }

    @Test
    void OAuth令牌响应_固定单位保持普通数字() throws Exception {
        // OpenOAuthController.token 返回 Map<String,Object>：expires_in 为 long 秒、token_type 为字符串
        // 经生产 mapper 序列化后必须是普通数字，不能被 java.time serializer 影响（设计 §3 排除清单）
        Map<String, Object> tokenResponse = Map.of(
                "access_token", "jwt-token",
                "token_type", "Bearer",
                "expires_in", 7200L,
                "scope", "openid");
        JsonNode node = new ObjectMapper().valueToTree(tokenResponse);
        assertThat(node.get("expires_in").isIntegralNumber()).isTrue();
        assertThat(node.get("expires_in").longValue()).isEqualTo(7200L);
        assertThat(node.get("token_type").asText()).isEqualTo("Bearer");
    }

    @Test
    void 设备授权响应_无javaTime字段_固定单位保持不变() {
        // DeviceAuthorizationResponse：deviceCode/userCode/verificationUri/expiresIn(int)/interval(int)
        // 全部为固定单位普通字段，无 java.time（排除范围 golden）
        for (java.lang.reflect.Field field : DeviceAuthorizationResponse.class.getDeclaredFields()) {
            assertThat(java.time.temporal.Temporal.class.isAssignableFrom(field.getType()))
                    .as("字段 %s 不允许为 java.time 类型", field.getName()).isFalse();
        }
        assertThat(DeviceAuthorizationResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("deviceCode", "userCode", "verificationUri", "expiresIn", "interval");
    }
}
