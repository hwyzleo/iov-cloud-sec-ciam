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
public class UserTag {
    private String tagId;
    private String userId;
    private String tagCode;
    private String tagName;
    private Integer tagStatus;
    private String tagSource;
    private Instant effectiveTime;
    private Instant expireTime;
    private String description;
}
