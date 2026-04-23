package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto2 {
    private String userId;
    private Integer userStatus;
    private String registerSource;
    private String registerChannel;
    private OffsetDateTime lastLoginTime;
    private OffsetDateTime createTime;
    private String nickname;
    private Integer gender;
    private String identityType;
    private String identityValue;
}
