package net.hwyz.iov.cloud.sec.ciam.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 风险等级枚举。
 * <p>
 * 对应表 {@code ciam_session.risk_level}、{@code ciam_risk_event.risk_level}（TINYINT）。
 */
@Getter
public enum RiskLevel implements CodeEnum {

    LOW(0, "低"),
    MEDIUM(1, "中"),
    HIGH(2, "高");

    private final int code;
    private final String description;

    RiskLevel(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RiskLevel fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的风险等级编码: " + code));
    }
}
