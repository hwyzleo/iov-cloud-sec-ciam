package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.open;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.OAuthAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.TokenAppService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.OidcDiscoveryDocument;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.OidcUserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OIDC 控制器 — UserInfo、Discovery Document、JWKS。
 */
@RestController
@RequestMapping("/api/open/oidc/v1")
@RequiredArgsConstructor
public class OpenOidcController extends BaseController {

    private final OAuthAppService oAuthAppService;
    private final TokenAppService tokenAppService;

    /** 获取 OIDC UserInfo */
    @GetMapping("/userinfo")
    public ApiResponse<OidcUserInfo> userInfo(@RequestParam String userId) {
        return ApiResponse.ok(oAuthAppService.getUserInfo(userId));
    }

    /** 获取 OIDC Discovery Document */
    @GetMapping("/.well-known/openid-configuration")
    public ApiResponse<OidcDiscoveryDocument> discoveryDocument() {
        return ApiResponse.ok(oAuthAppService.getDiscoveryDocument());
    }

    /** 获取 JWKS */
    @GetMapping("/jwks")
    public ApiResponse<Map<String, Object>> jwks() {
        return ApiResponse.ok(tokenAppService.getJwks());
    }
}
