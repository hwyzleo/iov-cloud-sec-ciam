package net.hwyz.iov.cloud.sec.ciam.service.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户身份 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityVo {
    
    @JsonProperty("identity_id")
    private String identityId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("identity_type")
    private String identityType;
    
    @JsonProperty("identity_value")
    private String identityValue;
    
    @JsonProperty("country_code")
    private String countryCode;
    
    @JsonProperty("verified_flag")
    private Integer verifiedFlag;
    
    @JsonProperty("primary_flag")
    private Integer primaryFlag;
    
    @JsonProperty("bind_source")
    private String bindSource;
    
    @JsonProperty("identity_status")
    private Integer identityStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("bind_time")
    private Instant bindTime;
}
