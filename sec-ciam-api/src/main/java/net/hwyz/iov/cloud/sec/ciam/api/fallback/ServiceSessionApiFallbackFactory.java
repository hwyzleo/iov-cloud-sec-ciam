package net.hwyz.iov.cloud.sec.ciam.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.api.service.ServiceSessionApi;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionValidateResult;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * CiamSessionService 降级工厂。
 */
@Slf4j
@Component
public class ServiceSessionApiFallbackFactory implements FallbackFactory<ServiceSessionApi> {

    @Override
    public ServiceSessionApi create(Throwable cause) {
        log.warn("[CIAM] CiamSessionService 调用失败，进入降级: {}", cause.getMessage());
        return sessionId -> SessionValidateResult.builder()
                .valid(false)
                .sessionId(sessionId)
                .build();
    }
}
