package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenDto {
    private String refreshTokenId;
    private String userId;
    private String sessionId;
    private String clientId;
    private String tokenFingerprint;
    private String parentTokenId;
    private Integer tokenStatus;
    private Instant issueTime;
    private Instant usedTime;
    private Instant revokeTime;
    private Instant expireTime;
    private String description;
}
