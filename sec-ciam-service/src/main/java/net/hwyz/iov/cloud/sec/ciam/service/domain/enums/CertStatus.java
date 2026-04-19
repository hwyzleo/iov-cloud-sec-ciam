package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 车主认证状态枚举。
 * <p>
 * 对应表 {@code ciam_owner_cert_state.cert_status}（TINYINT）。
 */
@Getter
public enum CertStatus implements CodeEnum {

    NOT_CERTIFIED(0, "未认证"),
    CERTIFYING(1, "认证中"),
    CERTIFIED(2, "已认证"),
    CERT_FAILED(3, "认证失败"),
    CERT_EXPIRED(4, "已失效");

    private final int code;
    private final String description;

    CertStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CertStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的认证状态编码: " + code));
    }
}
