package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivationRequestDto {
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
