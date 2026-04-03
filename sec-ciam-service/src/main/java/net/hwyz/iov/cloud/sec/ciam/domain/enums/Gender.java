package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 性别枚举。
 * <p>
 * 对应表 {@code ciam_user_profile.gender}（TINYINT）。
 */
@Getter
public enum Gender implements CodeEnum {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String description;

    Gender(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Gender fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的性别编码: " + code));
    }
}
