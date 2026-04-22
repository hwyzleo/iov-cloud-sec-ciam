package net.hwyz.iov.cloud.sec.ciam.service.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;

/**
 * 用户个人资料 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVo {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("real_name")
    private String realName;

    @JsonProperty("gender")
    private Integer gender;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @JsonProperty("birthday")
    private LocalDate birthday;

    @JsonProperty("region_code")
    private String regionCode;

    @JsonProperty("region_name")
    private String regionName;

    @JsonProperty("description")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("last_login_time")
    private Instant lastLoginTime;
}
