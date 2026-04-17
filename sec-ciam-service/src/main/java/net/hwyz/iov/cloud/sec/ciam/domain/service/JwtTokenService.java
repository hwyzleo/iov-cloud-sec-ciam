package net.hwyz.iov.cloud.sec.ciam.domain.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT Access Token 签发与校验领域服务。
 * <p>
 * 使用 RS256 签名算法，启动时生成内存 RSA 密钥对（开发/测试环境）。
 * 提供 Access Token 签发、校验与 JWKS 公钥输出能力。
 */
@Slf4j
@Service
public class JwtTokenService {

    static final String ISSUER = "https://account.openiov.com";
    static final String KEY_ID = "ciam-rsa-key-1";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public JwtTokenService() {
        KeyPair keyPair = generateRsaKeyPair();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        log.info("JwtTokenService 初始化完成，已生成内存 RSA 密钥对 (kid={})", KEY_ID);
    }

    /**
     * 供测试使用的构造函数，允许注入自定义密钥对。
     */
    JwtTokenService(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    /**
     * 签发 JWT Access Token。
     *
     * @param userId     用户业务唯一标识
     * @param deviceId   设备标识
     * @param scope      授权范围
     * @param sessionId  会话业务唯一标识
     * @param ttlSeconds 令牌有效期（秒）
     * @return JWT 字符串
     */
    public String generateAccessToken(String userId,
                                      String deviceId,
                                      String scope,
                                      String sessionId,
                                      int ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .subject(userId)
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim("device_id", deviceId)
                .claim("scope", scope)
                .claim("session_id", sessionId)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 签发 JWT Refresh Token。
     */
    public String generateRefreshToken(String userId, String deviceId, String scope, String sessionId) {
        Instant now = Instant.now();
        int ttlSeconds = 7 * 24 * 60 * 60;
        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .subject(userId)
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim("device_id", deviceId)
                .claim("scope", scope)
                .claim("session_id", sessionId)
                .claim("type", "refresh")
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 校验并解析 JWT Access Token。
     *
     * @param token JWT 字符串
     * @return 解析后的标准声明集
     * @throws BusinessException TOKEN_INVALID 签名无效、已过期或格式错误
     */
    public TokenClaims validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return TokenClaims.builder()
                    .sub(claims.getSubject())
                    .clientId(claims.get("client_id", String.class))
                    .scope(claims.get("scope", String.class))
                    .sessionId(claims.get("session_id", String.class))
                    .iss(claims.getIssuer())
                    .iat(claims.getIssuedAt().toInstant())
                    .exp(claims.getExpiration().toInstant())
                    .build();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, e);
        }
    }

    /**
     * 获取 JWKS（JSON Web Key Set）表示，用于 {@code /.well-known/jwks.json} 端点。
     *
     * @return JWKS 结构的 Map
     */
    public Map<String, Object> getJwks() {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", KEY_ID);
        jwk.put("n", base64UrlEncode(publicKey.getModulus().toByteArray()));
        jwk.put("e", base64UrlEncode(publicKey.getPublicExponent().toByteArray()));

        Map<String, Object> jwks = new LinkedHashMap<>();
        jwks.put("keys", List.of(jwk));
        return jwks;
    }

    // ---- 内部方法 ----

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA 密钥对生成失败", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        // 去除 BigInteger 前导零字节
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
