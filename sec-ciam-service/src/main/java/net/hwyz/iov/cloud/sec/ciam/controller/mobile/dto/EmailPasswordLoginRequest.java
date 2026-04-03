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
public class EmailPasswordLoginRequest {
    @NotBlank private String email;
    @NotBlank private String password;
    private String captchaId;
    private String captchaAnswer;
}
