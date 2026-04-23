package net.hwyz.iov.cloud.sec.ciam.api.service;

import net.hwyz.iov.cloud.sec.ciam.api.fallback.ServiceTokenApiFallbackFactory;
import net.hwyz.iov.cloud.sec.ciam.api.vo.TokenVerifyResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * CIAM Token 校验服务接口。
 * <p>
 * 外部服务引用 sec-ciam-api 依赖后，通过 OpenFeign 调用此接口完成 Token 校验。
 */
@FeignClient(name = "sec-ciam", contextId = "ciamTokenService", path = "/api/v1/service",
        fallbackFactory = ServiceTokenApiFallbackFactory.class)
public interface ServiceTokenApi {

    /**
     * 校验 Access Token 有效性并返回声明信息。
     *
     * @param accessToken 待校验的 Access Token
     * @return Token 校验结果
     */
    @PostMapping("/token/verify")
    TokenVerifyResult verifyToken(@RequestParam("accessToken") String accessToken);
}
