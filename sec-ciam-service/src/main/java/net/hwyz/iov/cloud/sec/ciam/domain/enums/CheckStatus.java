package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 校验状态枚举。
 * <p>
 * 对应表 {@code ciam_deactivation_request.check_status}（TINYINT）。
 */
@Getter
public enum CheckStatus implements CodeEnum {

    PENDING(0, "待校验"),
    PASSED(1, "通过"),
    FAILED(2, "不通过");

    private final int code;
    private final String description;

    CheckStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CheckStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的校验状态编码: " + code));
    }
}
