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
public class UpdateSensitiveFieldRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String field;
    @NotBlank
    private String value;
    @NotBlank
    private String verificationToken;
}
