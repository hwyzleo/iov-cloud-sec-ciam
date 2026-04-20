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
    private String consentScope;
    private Instant consentTime;
    private Instant withdrawTime;
    private String description;
}
