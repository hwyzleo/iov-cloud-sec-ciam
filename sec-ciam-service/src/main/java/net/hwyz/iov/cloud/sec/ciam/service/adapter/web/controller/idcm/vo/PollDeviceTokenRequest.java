package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.idcm.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollDeviceTokenRequest {
    @NotBlank
    private String clientId;
    @NotBlank
    private String deviceCode;
}
