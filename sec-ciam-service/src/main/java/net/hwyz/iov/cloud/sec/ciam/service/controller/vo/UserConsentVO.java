package net.hwyz.iov.cloud.sec.ciam.service.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户同意 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConsentVO {

    @JsonProperty("consent_id")
    private String consentId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("consent_type")
    private String consentType;

    @JsonProperty("consent_status")
    private Integer consentStatus;

    @JsonProperty("consent_scope")
    private String consentScope;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("consent_time")
    private Instant consentTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("withdraw_time")
    private Instant withdrawTime;

    @JsonProperty("description")
    private String description;
}
