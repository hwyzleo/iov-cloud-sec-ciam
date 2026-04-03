package net.hwyz.iov.cloud.sec.ciam.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.api.service.CiamTokenService;
import net.hwyz.iov.cloud.sec.ciam.api.vo.TokenVerifyResult;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * CiamTokenService 降级工厂。
 */
@Slf4j
@Component
public class CiamTokenServiceFallbackFactory implements FallbackFactory<CiamTokenService> {

    @Override
    public CiamTokenService create(Throwable cause) {
        log.warn("[CIAM] CiamTokenService 调用失败，进入降级: {}", cause.getMessage());
        return accessToken -> null;
    }
}
