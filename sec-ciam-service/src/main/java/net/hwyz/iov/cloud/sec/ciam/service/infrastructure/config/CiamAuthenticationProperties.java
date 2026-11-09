package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 手机认证配置属性（SEC-CIAM-DSN-CR-003 §7，RD-003-4/10）。
 * <p>
 * 配置契约：
 * <pre>
 * ciam:
 *   authentication:
 *     mobile:
 *       allowed-client-ids: ${CIAM_MOBILE_CLIENT_IDS:}      # 默认空：手机登录整体拒绝
 *     verification-code:
 *       fixed-enabled: ${CIAM_FIXED_VERIFICATION_CODE_ENABLED:false}  # 默认 false：不使用固定验证码
 * </pre>
 * 基础默认值必须是"安全关闭"：允许清单为空、固定验证码关闭。
 * 仅 dev/test 环境显式通过环境变量开启（{@code CIAM_MOBILE_CLIENT_IDS=ciam-mobile-app}、
 * {@code CIAM_FIXED_VERIFICATION_CODE_ENABLED=true}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ciam.authentication")
public class CiamAuthenticationProperties {

    /** 手机认证策略 */
    private Mobile mobile = new Mobile();

    /** 验证码策略 */
    private VerificationCode verificationCode = new VerificationCode();

    @Data
    public static class Mobile {

        /** 获准使用手机验证码认证的 OAuth 客户端标识允许清单 */
        private List<String> allowedClientIds = new ArrayList<>();

        /**
         * 返回去除空白后的允许清单，避免空配置被当作合法 clientId。
         */
        public List<String> effectiveAllowedClientIds() {
            if (allowedClientIds == null) {
                return List.of();
            }
            return allowedClientIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .toList();
        }
    }

    @Data
    public static class VerificationCode {

        /** 是否启用固定验证码 111111（仅 dev/test 允许为 true） */
        private boolean fixedEnabled = false;
    }
}
