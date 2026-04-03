package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 操作结果枚举。
 * <p>
 * 对应表 {@code ciam_audit_log.operation_result}（TINYINT）。
 */
@Getter
public enum OperationResult implements CodeEnum {

    FAILURE(0, "失败"),
    SUCCESS(1, "成功");

    private final int code;
    private final String description;

    OperationResult(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OperationResult fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的操作结果编码: " + code));
    }
}
