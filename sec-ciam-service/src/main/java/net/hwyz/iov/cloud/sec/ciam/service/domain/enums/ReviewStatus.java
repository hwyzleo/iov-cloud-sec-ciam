package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 审核状态枚举。
 * <p>
 * 对应表 {@code ciam_merge_request.review_status}、
 * {@code ciam_deactivation_request.review_status}（TINYINT）。
 * <p>
 * 注：合并申请额外包含"取消"状态（code=3），注销申请仅使用 0/1/2。
 */
@Getter
public enum ReviewStatus implements CodeEnum {

    PENDING(0, "待审"),
    APPROVED(1, "通过"),
    REJECTED(2, "驳回"),
    CANCELLED(3, "取消");

    private final int code;
    private final String description;

    ReviewStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ReviewStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的审核状态编码: " + code));
    }
}
