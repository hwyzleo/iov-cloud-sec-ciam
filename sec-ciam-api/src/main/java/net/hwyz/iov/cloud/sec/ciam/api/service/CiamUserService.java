package net.hwyz.iov.cloud.sec.ciam.api.service;

import net.hwyz.iov.cloud.sec.ciam.api.fallback.CiamUserServiceFallbackFactory;
import net.hwyz.iov.cloud.sec.ciam.api.vo.UserBasicInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * CIAM 用户信息查询服务接口。
 * <p>
 * 外部服务引用 sec-ciam-api 依赖后，通过 OpenFeign 调用此接口查询用户基础信息。
 */
@FeignClient(name = "sec-ciam", contextId = "ciamUserService", path = "/api/v1/service",
        fallbackFactory = CiamUserServiceFallbackFactory.class)
public interface CiamUserService {

    /**
     * 根据用户 ID 查询基础信息。
     *
     * @param userId 用户业务唯一标识
     * @return 用户基础信息
     */
    @GetMapping("/user/info")
    UserBasicInfo getUserInfo(@RequestParam("userId") String userId);
}
