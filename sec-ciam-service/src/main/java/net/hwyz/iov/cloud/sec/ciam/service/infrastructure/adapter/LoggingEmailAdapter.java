package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import org.springframework.stereotype.Component;

/**
 * 邮件发送适配器 — 日志桩实现（开发环境使用）。
 */
@Slf4j
@Component
public class LoggingEmailAdapter implements EmailAdapter {

    @Override
    public AdapterResult sendVerificationCode(String email, String code) {
        log.info("[EMAIL-STUB] 发送验证码: email={}, code={}", email, code);
        return AdapterResult.ok("stub-email-sent");
    }
}
