package net.hwyz.iov.cloud.sec.ciam.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.api.service.ServiceUserApi;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * CiamUserService 降级工厂。
 */
@Slf4j
@Component
public class ServiceUserApiFallbackFactory implements FallbackFactory<ServiceUserApi> {

    @Override
    public ServiceUserApi create(Throwable cause) {
        log.warn("[CIAM] CiamUserService 调用失败，进入降级: {}", cause.getMessage());
        return userId -> null;
    }
}
