package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gateway.http;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.SmsAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 短信发送适配器 — 日志桩实现（仅 dev/test 装配；生产缺少真实适配器时装配失败，SEC-CIAM-DSN-CR-003 §7）。
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class LoggingSmsAdapter implements SmsAdapter {

    @Override
    public AdapterResult sendVerificationCode(String mobile, String countryCode, String code) {
        log.info("[SMS-STUB] 发送验证码: mobile={}, countryCode={}, code={}", mobile, countryCode, code);
        return AdapterResult.ok("stub-sms-sent");
    }
}
