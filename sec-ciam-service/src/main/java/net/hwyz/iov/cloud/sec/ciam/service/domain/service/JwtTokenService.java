package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * JWT Access Token 签发与校验领域服务。
 * <p>
 * 使用 RS256 签名算法，密钥持久化至数据库。
 * 启动时从数据库加载主密钥，如无则生成新密钥并存储。
 * 提供 Access Token 签发、校验与 JWKS 公钥输出能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    static final String ISSUER = "https://account.openiov.com";

    private final JwkDomainService jwkDomainService;

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
        RSAPrivateKey privateKey = jwkDomainService.getPrimaryPrivateKey();
        String keyId = jwkDomainService.getKeyId();

        return Jwts.builder()
                .header().keyId(keyId).and()
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
        RSAPrivateKey privateKey = jwkDomainService.getPrimaryPrivateKey();
        String keyId = jwkDomainService.getKeyId();

        return Jwts.builder()
                .header().keyId(keyId).and()
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
                    .verifyWith(getPublicKeyForVerification(token))
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
        } catch (NoSuchElementException e) {
            throw new BusinessException(CiamErrorCode.TOKEN_INVALID, "密钥不存在：" + e.getMessage());
        }
    }

    /**
     * 获取 JWKS（JSON Web Key Set）表示，用于 {@code /.well-known/jwks.json} 端点。
     *
     * @return JWKS 结构的 Map
     */
    public Map<String, Object> getJwks() {
        List<JwkPo> activeKeys = jwkDomainService.getAllActiveKeys();
        List<Map<String, Object>> keys = activeKeys.stream()
                .map(this::convertToJwk)
                .toList();

        Map<String, Object> jwks = new LinkedHashMap<>();
        jwks.put("keys", keys);
        return jwks;
    }

    // ---- 内部方法 ----

    /**
     * 根据 token header 中的 kid 获取对应的公钥。
     */
    private RSAPublicKey getPublicKeyForVerification(String token) {
        try {
            String keyId = extractKeyId(token);
            if (keyId != null) {
                return jwkDomainService.getPublicKeyByKeyId(keyId);
            }
            return jwkDomainService.getPrimaryPublicKey();
        } catch (Exception e) {
            throw new JwtException("无法提取密钥 ID", e);
        }
    }

    /**
     * 从 token 中提取密钥 ID (kid)。
     */
    private String extractKeyId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 1) {
            return null;
        }
        try {
            String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            com.fasterxml.jackson.databind.JsonNode header = 
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(headerJson);
            return header.has("kid") ? header.get("kid").asText() : null;
        } catch (Exception e) {
            throw new JwtException("无法解析 token header", e);
        }
    }

    /**
     * 将 JwkPo 转换为 JWK 格式。
     */
    private Map<String, Object> convertToJwk(JwkPo entity) {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", entity.getKeyId());

        byte[] modulusBytes = Base64.getMimeDecoder().decode(entity.getPublicKeyPem());
        try {
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.spec.X509EncodedKeySpec keySpec =
                    new java.security.spec.X509EncodedKeySpec(modulusBytes);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

            jwk.put("n", base64UrlEncode(publicKey.getModulus().toByteArray()));
            jwk.put("e", base64UrlEncode(publicKey.getPublicExponent().toByteArray()));
        } catch (Exception e) {
            throw new IllegalStateException("JWK 转换失败：keyId=" + entity.getKeyId(), e);
        }

        return jwk;
    }

    private static String base64UrlEncode(byte[] bytes) {
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
