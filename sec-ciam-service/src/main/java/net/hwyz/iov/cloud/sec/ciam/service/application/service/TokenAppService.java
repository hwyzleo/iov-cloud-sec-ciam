package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.vo.TokenVerifyResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TokenClaims;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Token 应用服务 — 封装 Token 校验与 JWKS 获取逻辑。
 */
@Service
@RequiredArgsConstructor
public class TokenAppService {

    private final JwtTokenService jwtTokenService;

    /**
     * 校验 Token 并返回校验结果。
     *
     * @param accessToken 访问令牌
     * @return 令牌校验结果 VO
     */
    public TokenVerifyResult verifyToken(String accessToken) {
        TokenClaims claims = jwtTokenService.validateAccessToken(accessToken);
        return TokenVerifyResult.builder()
                .userId(claims.getSub())
                .clientId(claims.getClientId())
                .scope(claims.getScope())
                .claims(Map.of(
                        "sub", claims.getSub() != null ? claims.getSub() : "",
                        "client_id", claims.getClientId() != null ? claims.getClientId() : "",
                        "scope", claims.getScope() != null ? claims.getScope() : "",
                        "session_id", claims.getSessionId() != null ? claims.getSessionId() : ""
                ))
                .build();
    }

    /**
     * 获取 JWKS。
     *
     * @return JWKS Map
     */
    public Map<String, Object> getJwks() {
        return jwtTokenService.getJwks();
    }
}
