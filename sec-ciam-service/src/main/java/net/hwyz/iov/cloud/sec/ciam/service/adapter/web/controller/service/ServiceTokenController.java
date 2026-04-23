package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamTokenService;
import net.hwyz.iov.cloud.sec.ciam.api.vo.TokenVerifyResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.TokenClaims;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 服务接口 — Token 校验。
 */
@RestController
@RequestMapping("/api/service/token/v1")
@RequiredArgsConstructor
public class ServiceTokenController implements CiamTokenService {

    private final JwtTokenService jwtTokenService;

    @Override
    public TokenVerifyResult verifyToken(@RequestParam("accessToken") String accessToken) {
        TokenClaims claims = jwtTokenService.validateAccessToken(accessToken);
        return TokenVerifyResult.builder()
                .userId(claims.getSub())
                .clientId(claims.getClientId())
                .scope(claims.getScope())
                .claims(Map.of(
                        "sub", claims.getSub(),
                        "client_id", claims.getClientId() != null ? claims.getClientId() : "",
                        "scope", claims.getScope() != null ? claims.getScope() : "",
                        "session_id", claims.getSessionId() != null ? claims.getSessionId() : ""
                ))
                .build();
    }
}
