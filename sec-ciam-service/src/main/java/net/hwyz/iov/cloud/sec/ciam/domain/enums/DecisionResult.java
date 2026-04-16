package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 风险处置结果枚举。
 * <p>
 * 对应表 {@code ciam_risk_event.decision_result}（VARCHAR(32)）。
 */
@Getter
public enum DecisionResult implements LabelEnum {

    ALLOW("allow", "放行"),
    CHALLENGE("challenge", "挑战"),
    BLOCK("block", "阻断"),
    KICKOUT("kickout", "强制下线");

    private final String code;
    private final String description;

    DecisionResult(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DecisionResult fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的处置结果: " + code));
    }
}
