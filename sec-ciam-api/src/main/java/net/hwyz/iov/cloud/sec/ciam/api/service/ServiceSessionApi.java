package net.hwyz.iov.cloud.sec.ciam.api.service;

import net.hwyz.iov.cloud.sec.ciam.api.fallback.ServiceSessionApiFallbackFactory;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionValidateResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * CIAM 会话校验服务接口。
 * <p>
 * 外部服务引用 sec-ciam-api 依赖后，通过 OpenFeign 调用此接口完成会话校验。
 */
@FeignClient(name = "sec-ciam", contextId = "ciamSessionService", path = "/api/v1/service",
        fallbackFactory = ServiceSessionApiFallbackFactory.class)
public interface ServiceSessionApi {

    /**
     * 校验会话是否有效。
     *
     * @param sessionId 会话 ID
     * @return 会话校验结果
     */
    @GetMapping("/session/validate")
    SessionValidateResult validateSession(@RequestParam("sessionId") String sessionId);
}
