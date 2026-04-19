package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.LocalMobileAuthAdapter;
import org.springframework.stereotype.Component;

/**
 * 本机手机号认证适配器 — 桩实现（开发环境使用）。
 */
@Slf4j
@Component
public class StubLocalMobileAuthAdapter implements LocalMobileAuthAdapter {

    @Override
    public String verifyToken(String token) {
        log.info("[LOCAL-MOBILE-STUB] 验证本机号码认证 token: token={}", token);
        return "13800000001";
    }
}
