package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.open.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRequest {
    @NotBlank
    private String grantType;
    @NotBlank
    private String clientId;
    private String clientSecret;
    private String code;
    private String redirectUri;
    private String codeVerifier;
    private String refreshToken;
    private String scope;
    private String deviceCode;
}
