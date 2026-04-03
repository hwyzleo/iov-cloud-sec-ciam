package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 凭据状态枚举。
 * <p>
 * 对应表 {@code ciam_user_credential.credential_status}（TINYINT）。
 */
@Getter
public enum CredentialStatus implements CodeEnum {

    INVALID(0, "失效"),
    VALID(1, "有效");

    private final int code;
    private final String description;

    CredentialStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CredentialStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的凭据状态编码: " + code));
    }
}
