package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MFA 挑战领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaChallenge {
    private String challengeId;
    private String userId;
    private String sessionId;
    private String challengeType;
    private String challengeScene;
    private String receiverMask;
    private String verifyCodeHash;
    private Instant sendTime;
    private Instant expireTime;
    private Instant verifyTime;
    private Integer challengeStatus;
    private String riskEventId;
    private String description;
}
