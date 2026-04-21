package net.hwyz.iov.cloud.sec.ciam.service.controller.open.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizeRequest {
    @NotBlank
    private String clientId;
    @NotBlank
    private String userId;
    private String sessionId;
    @NotBlank
    private String redirectUri;
    private String scope;
    private String codeChallenge;
    private String challengeMethod;
}
