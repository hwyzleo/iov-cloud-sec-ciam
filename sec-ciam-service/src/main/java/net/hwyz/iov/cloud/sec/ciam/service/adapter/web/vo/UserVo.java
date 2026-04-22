package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVo {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_status")
    private Integer userStatus;

    @JsonProperty("brand_code")
    private String brandCode;

    @JsonProperty("register_source")
    private String registerSource;

    @JsonProperty("register_channel")
    private String registerChannel;

    @JsonProperty("primary_identity_type")
    private String primaryIdentityType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("last_login_time")
    private Instant lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("deactivated_time")
    private Instant deactivatedTime;

    @JsonProperty("description")
    private String description;
}
