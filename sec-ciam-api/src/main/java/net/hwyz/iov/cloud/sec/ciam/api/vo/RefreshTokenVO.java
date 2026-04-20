package net.hwyz.iov.cloud.sec.ciam.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 刷新令牌 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenVO {
    
    @JsonProperty("refresh_token_id")
    private String refreshTokenId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("session_id")
    private String sessionId;
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("token_status")
    private Integer tokenStatus;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("issue_time")
    private Instant issueTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("used_time")
    private Instant usedTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("expire_time")
    private Instant expireTime;
}
