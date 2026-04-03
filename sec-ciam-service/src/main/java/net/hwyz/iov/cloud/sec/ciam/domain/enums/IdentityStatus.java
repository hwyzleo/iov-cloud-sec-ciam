package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 登录标识状态枚举。
 * <p>
 * 对应表 {@code ciam_user_identity.identity_status}（TINYINT）。
 */
@Getter
public enum IdentityStatus implements CodeEnum {

    UNBOUND(0, "已解绑"),
    BOUND(1, "已绑定");

    private final int code;
    private final String description;

    IdentityStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static IdentityStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的标识状态编码: " + code));
    }
}
