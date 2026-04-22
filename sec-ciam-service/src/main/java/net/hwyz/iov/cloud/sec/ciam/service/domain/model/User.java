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
public class User {
    private String userId;
    private Integer userStatus;
    private String brandCode;
    private String registerSource;
    private String registerChannel;
    private String primaryIdentityType;
    private Instant lastLoginTime;
    private Instant deactivatedTime;
    private String description;
    private Instant createTime;
    private Instant modifyTime;
}
