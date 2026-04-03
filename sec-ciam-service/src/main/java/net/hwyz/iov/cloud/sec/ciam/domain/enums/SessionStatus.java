package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 会话状态枚举。
 * <p>
 * 对应表 {@code ciam_session.session_status}（TINYINT）。
 */
@Getter
public enum SessionStatus implements CodeEnum {

    INVALID(0, "失效"),
    ACTIVE(1, "有效"),
    KICKED(2, "下线"),
    EXPIRED(3, "过期");

    private final int code;
    private final String description;

    SessionStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static SessionStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的会话状态编码: " + code));
    }
}
