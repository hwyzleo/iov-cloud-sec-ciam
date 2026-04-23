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
public class MergeRequestDto {
    private String mergeRequestId;
    private String sourceUserId;
    private String targetUserId;
    private Integer reviewStatus;
    private String requestSource;
    private Instant requestTime;
    private Instant reviewTime;
    private String reviewer;
    private String remark;
    private String description;
}
