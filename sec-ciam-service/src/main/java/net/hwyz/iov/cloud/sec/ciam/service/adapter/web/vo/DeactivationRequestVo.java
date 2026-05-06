package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 账号注销申请 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivationRequestVo {

    private String deactivationRequestId;

    private String userId;

    private Integer reviewStatus;

    private String requestReason;

    private String remark;

    private Instant requestTime;

    private Instant reviewTime;

    private String reviewer;

    private String description;
}