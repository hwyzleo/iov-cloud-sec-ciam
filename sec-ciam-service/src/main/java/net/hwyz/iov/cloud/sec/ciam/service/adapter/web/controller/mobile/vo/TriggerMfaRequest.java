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
public class TriggerMfaRequest {
    @NotBlank
    private String userId;
    private String sessionId;
    @NotBlank
    private String challengeType;
    @NotBlank
    private String challengeScene;
    private String receiverMask;
    private String riskEventId;
}
