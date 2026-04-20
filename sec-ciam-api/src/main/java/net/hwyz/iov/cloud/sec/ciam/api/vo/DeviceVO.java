package net.hwyz.iov.cloud.sec.ciam.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceVO {
    
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("client_type")
    private String clientType;
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("app_version")
    private String appVersion;
    
    @JsonProperty("device_name")
    private String deviceName;
    
    @JsonProperty("device_os")
    private String deviceOs;
    
    @JsonProperty("device_fingerprint")
    private String deviceFingerprint;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("trusted_flag")
    private Integer trustedFlag;
    
    @JsonProperty("device_status")
    private Integer deviceStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("first_login_time")
    private Instant firstLoginTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("last_login_time")
    private Instant lastLoginTime;
}
