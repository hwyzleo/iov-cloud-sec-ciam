package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamTokenService;
import net.hwyz.iov.cloud.sec.ciam.api.vo.TokenVerifyResult;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.TokenAppService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务接口 — Token 校验。
 */
@RestController
@RequestMapping("/api/service/token/v1")
@RequiredArgsConstructor
public class ServiceTokenController implements CiamTokenService {

    private final TokenAppService tokenAppService;

    @Override
    public TokenVerifyResult verifyToken(@RequestParam("accessToken") String accessToken) {
        return tokenAppService.verifyToken(accessToken);
    }
}
