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
public class MergeRequest {
    private String mergeRequestId;
    private String sourceUserId;
    private String targetUserId;
    private String conflictIdentityType;
    private String conflictIdentityHash;
    private String applySource;
    private Integer reviewStatus;
    private String reviewer;
    private Instant reviewTime;
    private String reviewOpinion;
    private Integer executeStatus;
    private String finalUserId;
    private Instant finishTime;
    private Integer rowVersion;
    private Integer rowValid;
    private Instant createTime;
    private Instant modifyTime;
}
