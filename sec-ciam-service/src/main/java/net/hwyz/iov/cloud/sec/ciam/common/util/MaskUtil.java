package net.hwyz.iov.cloud.sec.ciam.common.util;

/**
 * 数据脱敏工具类。
 * <p>
 * 对手机号、邮箱、姓名等敏感信息进行掩码处理，用于日志输出和接口展示。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，中间用 **** 替代。
     * <p>示例：138****1234</p>
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏：保留首字符和 @ 后域名，中间用 *** 替代。
     * <p>示例：t***@example.com</p>
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * 姓名脱敏：保留首字，其余用 * 替代。
     * <p>示例：张** </p>
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
