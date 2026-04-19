package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 认证标签状态枚举。
 * <p>
 * 对应表 {@code ciam_user_tag.tag_status}（TINYINT）。
 */
@Getter
public enum TagStatus implements CodeEnum {

    INVALID(0, "失效"),
    VALID(1, "生效");

    private final int code;
    private final String description;

    TagStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TagStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的标签状态编码: " + code));
    }
}
