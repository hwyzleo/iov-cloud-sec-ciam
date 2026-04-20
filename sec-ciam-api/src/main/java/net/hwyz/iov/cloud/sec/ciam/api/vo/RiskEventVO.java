package net.hwyz.iov.cloud.sec.ciam.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 风险事件 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEventVO {
    
    @JsonProperty("risk_event_id")
    private String riskEventId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("risk_scene")
    private String riskScene;
    
    @JsonProperty("risk_type")
    private String riskType;
    
    @JsonProperty("risk_level")
    private Integer riskLevel;
    
    @JsonProperty("decision_result")
    private String decisionResult;
    
    @JsonProperty("hit_rules")
    private String hitRules;
    
    @JsonProperty("handled_flag")
    private Integer handledFlag;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("event_time")
    private Instant eventTime;
}
