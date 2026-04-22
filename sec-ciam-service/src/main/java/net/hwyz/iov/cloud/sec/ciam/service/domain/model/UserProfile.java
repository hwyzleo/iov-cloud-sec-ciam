package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String profileId;
    private String userId;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private Integer gender;
    private LocalDate birthday;
    private String regionCode;
    private String regionName;
    private String description;
    private Instant lastLoginTime;
}
