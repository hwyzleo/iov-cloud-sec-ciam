package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {

    private String userId;

    private Integer userStatus;

    private String registerSource;

    private String registerChannel;

    private Instant lastLoginTime;

    private Instant createTime;

    private String nickname;

    private Integer gender;

    private String identityType;

    private String identityValue;
}