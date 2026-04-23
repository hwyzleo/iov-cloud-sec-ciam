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
public class UserConsent {
    private String consentId;
    private String userId;
    private String consentType;
    private Integer consentStatus;
    private String policyVersion;
    private String sourceChannel;
    private String clientType;
    private String operateIp;
    private Instant operateTime;
    private String description;
}
