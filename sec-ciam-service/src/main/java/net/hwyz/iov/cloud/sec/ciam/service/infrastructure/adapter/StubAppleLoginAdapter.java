package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AppleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.ThirdPartyUserInfo;
import org.springframework.stereotype.Component;

/**
 * Apple 登录适配器 — 桩实现（开发环境使用）。
 */
@Slf4j
@Component
public class StubAppleLoginAdapter implements AppleLoginAdapter {

    @Override
    public ThirdPartyUserInfo verifyIdentityToken(String identityToken) {
        log.info("[APPLE-STUB] 验证 identity token: token={}", identityToken);
        return ThirdPartyUserInfo.builder()
                .subject("apple-stub-subject-" + identityToken.hashCode())
                .email("stub@apple.example.com")
                .build();
    }
}
