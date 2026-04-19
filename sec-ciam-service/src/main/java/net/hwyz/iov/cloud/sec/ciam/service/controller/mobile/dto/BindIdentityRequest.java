package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BindIdentityRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String identityType;
    @NotBlank
    private String identityValue;
    private String countryCode;
    private String bindSource;
}
