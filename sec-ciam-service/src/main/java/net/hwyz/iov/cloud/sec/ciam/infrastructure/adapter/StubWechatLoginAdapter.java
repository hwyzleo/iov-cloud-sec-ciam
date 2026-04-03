package net.hwyz.iov.cloud.sec.ciam.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.ThirdPartyUserInfo;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.WechatLoginAdapter;
import org.springframework.stereotype.Component;

/**
 * 微信登录适配器 — 桩实现（开发环境使用）。
 */
@Slf4j
@Component
public class StubWechatLoginAdapter implements WechatLoginAdapter {

    @Override
    public ThirdPartyUserInfo getUserInfo(String code) {
        log.info("[WECHAT-STUB] 获取用户信息: code={}", code);
        return ThirdPartyUserInfo.builder()
                .subject("wx-stub-openid-" + code)
                .unionId("wx-stub-unionid-" + code)
                .nickname("微信用户")
                .avatarUrl("https://stub.example.com/avatar.png")
                .build();
    }
}
