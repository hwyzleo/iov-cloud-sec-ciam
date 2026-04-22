package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredential {
    private String credentialId;
    private String userId;
    private String credentialType;
    private String credentialHash;
    private String salt;
    private String hashAlgorithm;
    private Instant passwordSetTime;
    private Instant lastVerifyTime;
    private Integer failCount;
    private Instant lockedUntil;
    private Integer credentialStatus;
    private String description;
}
