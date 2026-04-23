package net.hwyz.iov.cloud.sec.ciam.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamSessionService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * CiamSessionService 降级工厂。
 */
@Slf4j
@Component
public class CiamSessionServiceFallbackFactory implements FallbackFactory<CiamSessionService> {

    @Override
    public CiamSessionService create(Throwable cause) {
        log.warn("[CIAM] CiamSessionService 调用失败，进入降级: {}", cause.getMessage());
        return sessionId -> null;
    }
}
