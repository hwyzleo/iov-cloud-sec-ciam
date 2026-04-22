package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gatewayimpl.http;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import org.springframework.stereotype.Component;

/**
 * 短信发送适配器 — 日志桩实现（开发环境使用）。
 */
@Slf4j
@Component
public class LoggingSmsAdapter implements SmsAdapter {

    @Override
    public AdapterResult sendVerificationCode(String mobile, String countryCode, String code) {
        log.info("[SMS-STUB] 发送验证码: mobile={}, countryCode={}, code={}", mobile, countryCode, code);
        return AdapterResult.ok("stub-sms-sent");
    }
}
