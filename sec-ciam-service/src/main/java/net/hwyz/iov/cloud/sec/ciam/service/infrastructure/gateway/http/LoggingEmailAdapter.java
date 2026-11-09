package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.http;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.EmailAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 邮件发送适配器 — 日志桩实现（仅 dev/test 装配；生产缺少真实适配器时装配失败，SEC-CIAM-DSN-CR-003 §7）。
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class LoggingEmailAdapter implements EmailAdapter {

    @Override
    public AdapterResult sendVerificationCode(String email, String code) {
        log.info("[EMAIL-STUB] 发送验证码: email={}, code={}", email, code);
        return AdapterResult.ok("stub-email-sent");
    }
}
