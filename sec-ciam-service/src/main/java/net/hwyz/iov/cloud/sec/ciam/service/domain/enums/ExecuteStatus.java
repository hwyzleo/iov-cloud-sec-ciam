package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 执行状态枚举。
 * <p>
 * 对应表 {@code ciam_deactivation_request.execute_status}（TINYINT）。
 */
@Getter
public enum ExecuteStatus implements CodeEnum {

    PENDING(0, "待执行"),
    EXECUTED(1, "已执行"),
    FAILED(2, "失败");

    private final int code;
    private final String description;

    ExecuteStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ExecuteStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的执行状态编码: " + code));
    }
}
