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
public class UserConsentDto2 {
    private String consentId;
    private String userId;
    private String consentType;
    private Integer consentStatus;
    private String consentScope;
    private Instant consentTime;
    private Instant withdrawTime;
    private String description;
}
