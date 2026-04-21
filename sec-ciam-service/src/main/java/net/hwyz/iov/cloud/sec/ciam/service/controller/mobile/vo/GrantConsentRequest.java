package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrantConsentRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String consentType;
    private String policyVersion;
    private String sourceChannel;
    private String clientType;
    private String operateIp;
}
