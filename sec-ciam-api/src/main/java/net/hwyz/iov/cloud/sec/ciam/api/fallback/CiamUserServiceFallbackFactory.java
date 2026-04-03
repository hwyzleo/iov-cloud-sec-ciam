package net.hwyz.iov.cloud.sec.ciam.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamUserService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * CiamUserService 降级工厂。
 */
@Slf4j
@Component
public class CiamUserServiceFallbackFactory implements FallbackFactory<CiamUserService> {

    @Override
    public CiamUserService create(Throwable cause) {
        log.warn("[CIAM] CiamUserService 调用失败，进入降级: {}", cause.getMessage());
        return userId -> null;
    }
}
