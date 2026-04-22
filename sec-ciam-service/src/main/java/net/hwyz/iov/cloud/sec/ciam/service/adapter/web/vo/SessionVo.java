package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 会话 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVo {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_type")
    private String clientType;

    @JsonProperty("login_ip")
    private String loginIp;

    @JsonProperty("login_region")
    private String loginRegion;

    @JsonProperty("risk_level")
    private Integer riskLevel;

    @JsonProperty("session_status")
    private Integer sessionStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("login_time")
    private Instant loginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("last_active_time")
    private Instant lastActiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("expire_time")
    private Instant expireTime;

    @JsonProperty("description")
    private String description;
}
