package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceVo {

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_type")
    private String clientType;

    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("device_model")
    private String deviceModel;

    @JsonProperty("os_name")
    private String osName;

    @JsonProperty("os_version")
    private String osVersion;

    @JsonProperty("app_version")
    private String appVersion;

    @JsonProperty("language")
    private String language;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("device_status")
    private Integer deviceStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("last_login_time")
    private Instant lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("create_time")
    private Instant createTime;
}
