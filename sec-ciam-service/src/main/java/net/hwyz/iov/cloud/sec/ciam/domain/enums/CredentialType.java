package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 凭据类型枚举。
 * <p>
 * 对应表 {@code ciam_user_credential.credential_type}（VARCHAR(32)）。
 */
@Getter
public enum CredentialType implements LabelEnum {

    EMAIL_PASSWORD("email_password", "邮箱密码");

    private final String value;
    private final String description;

    CredentialType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static CredentialType fromValue(String value) {
        return Arrays.stream(values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的凭据类型: " + value));
    }
}
