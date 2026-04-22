package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivationRequest {
    private String deactivationRequestId;
    private String userId;
    private String requestSource;
    private String requestReason;
    private Integer checkStatus;
    private Integer reviewStatus;
    private String reviewer;
    private Instant reviewTime;
    private String reviewOpinion;
    private Integer executeStatus;
    private Instant executeTime;
    private Instant requestedTime;
    private Integer retainAuditOnly;
    private Integer rowVersion;
    private Integer rowValid;
    private Instant createTime;
    private Instant modifyTime;
}
