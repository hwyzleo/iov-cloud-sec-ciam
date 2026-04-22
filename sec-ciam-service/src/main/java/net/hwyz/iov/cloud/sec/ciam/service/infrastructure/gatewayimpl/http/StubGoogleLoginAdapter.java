package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gatewayimpl.http;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.GoogleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.ThirdPartyUserInfo;
import org.springframework.stereotype.Component;

/**
 * Google 登录适配器 — 桩实现（开发环境使用）。
 */
@Slf4j
@Component
public class StubGoogleLoginAdapter implements GoogleLoginAdapter {

    @Override
    public ThirdPartyUserInfo verifyIdToken(String idToken) {
        log.info("[GOOGLE-STUB] 验证 ID token: token={}", idToken);
        return ThirdPartyUserInfo.builder()
                .subject("google-stub-subject-" + idToken.hashCode())
                .email("stub@google.example.com")
                .build();
    }
}
