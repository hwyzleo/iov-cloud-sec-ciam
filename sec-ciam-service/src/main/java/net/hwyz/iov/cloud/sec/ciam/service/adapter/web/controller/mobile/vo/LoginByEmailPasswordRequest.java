package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginByEmailPasswordRequest {
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String clientId;
    private String captchaId;
    private String captchaAnswer;
}
