package net.hwyz.iov.cloud.sec.ciam.controller.open;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.domain.service.JwtTokenService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.OidcDiscoveryDocument;
import net.hwyz.iov.cloud.sec.ciam.domain.service.OidcService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.OidcUserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OIDC 控制器 — UserInfo、Discovery Document、JWKS。
 */
@RestController
@RequestMapping("/api/open/v1/oidc")
@RequiredArgsConstructor
public class OpenOidcController {

    private final OidcService oidcService;
    private final JwtTokenService jwtTokenService;

    /** 获取 OIDC UserInfo */
    @GetMapping("/userinfo")
    public ApiResponse<OidcUserInfo> userInfo(@RequestParam String userId) {
        OidcUserInfo info = oidcService.getUserInfo(userId);
        return ApiResponse.ok(info);
    }

    /** 获取 OIDC Discovery Document */
    @GetMapping("/.well-known/openid-configuration")
    public ApiResponse<OidcDiscoveryDocument> discoveryDocument() {
        OidcDiscoveryDocument doc = oidcService.getDiscoveryDocument();
        return ApiResponse.ok(doc);
    }

    /** 获取 JWKS */
    @GetMapping("/jwks")
    public ApiResponse<Map<String, Object>> jwks() {
        Map<String, Object> jwks = jwtTokenService.getJwks();
        return ApiResponse.ok(jwks);
    }
}
