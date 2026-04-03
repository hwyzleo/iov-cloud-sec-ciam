package net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMobileCodeRequest {
    @NotBlank private String mobile;
    private String countryCode;
}
