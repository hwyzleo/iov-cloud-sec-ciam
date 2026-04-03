package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 用户状态枚举。
 * <p>
 * 对应表 {@code ciam_user.user_status}（TINYINT）。
 */
@Getter
public enum UserStatus implements CodeEnum {

    PENDING(0, "待验证"),
    ACTIVE(1, "正常"),
    LOCKED(2, "已锁定"),
    DISABLED(3, "已禁用"),
    DEACTIVATING(4, "注销处理中"),
    DEACTIVATED(5, "已注销");

    private final int code;
    private final String description;

    UserStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的用户状态编码: " + code));
    }
}
