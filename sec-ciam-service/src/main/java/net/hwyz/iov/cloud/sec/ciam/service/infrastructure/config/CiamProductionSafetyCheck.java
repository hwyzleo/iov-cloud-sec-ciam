package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.SmsAdapter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 手机认证生产安全启动检查（SEC-CIAM-DSN-CR-003 §7，RD-003-10）。
 * <p>
 * 在装配完成后执行 fail-fast 校验，阻止把开发/测试机制误认为生产能力：
 * <ol>
 *   <li>固定验证码 {@code ciam.authentication.verification-code.fixed-enabled=true}
 *       只允许在非生产（非 {@code prod}）环境出现；{@code prod} 下启动失败。</li>
 *   <li>日志桩短信/邮件适配器仅注册于非生产（{@code @Profile("!prod")}），
 *       因此 {@code prod} 缺少真实适配器时依赖装配本身即失败，无需额外判断。</li>
 * </ol>
 * 注意：本 CR 不提供生产短信适配器，因此生产手机认证发布仍保持阻断（§7、§13）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CiamProductionSafetyCheck implements InitializingBean {

    private static final String PRODUCTION_PROFILE = "prod";

    private final CiamAuthenticationProperties authenticationProperties;
    private final Environment environment;
    private final SmsAdapter smsAdapter;
    private final EmailAdapter emailAdapter;

    @Override
    public void afterPropertiesSet() {
        if (!authenticationProperties.getVerificationCode().isFixedEnabled()) {
            return;
        }

        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean production = activeProfiles.stream()
                .anyMatch(profile -> PRODUCTION_PROFILE.equalsIgnoreCase(profile));
        if (production) {
            throw new IllegalStateException(
                    "固定验证码模式禁止在生产环境启用（当前 profile=" + activeProfiles + "）。"
                            + "生产环境启用固定验证码会阻断认证安全验收，启动终止。");
        }

        log.warn("固定验证码模式已启用：仅限开发/测试使用，生产发布保持阻断。"
                        + "smsAdapter={}, emailAdapter={}, profiles={}",
                smsAdapter.getClass().getSimpleName(),
                emailAdapter.getClass().getSimpleName(),
                activeProfiles);
    }
}
