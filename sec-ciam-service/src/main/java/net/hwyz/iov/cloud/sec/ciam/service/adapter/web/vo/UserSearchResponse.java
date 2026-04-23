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

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_status")
    private Integer userStatus;

    @JsonProperty("register_source")
    private String registerSource;

    @JsonProperty("register_channel")
    private String registerChannel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("last_login_time")
    private Instant lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("create_time")
    private Instant createTime;

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("gender")
    private Integer gender;

    @JsonProperty("identity_type")
    private String identityType;

    @JsonProperty("identity_value")
    private String identityValue;
}