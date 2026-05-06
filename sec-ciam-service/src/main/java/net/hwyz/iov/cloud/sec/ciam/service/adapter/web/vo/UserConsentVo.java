package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户同意 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConsentVo {

    private String consentId;

    private String userId;

    private String consentType;

    private Integer consentStatus;

    private String consentScope;

    private Instant consentTime;

    private Instant withdrawTime;

    private String description;
}