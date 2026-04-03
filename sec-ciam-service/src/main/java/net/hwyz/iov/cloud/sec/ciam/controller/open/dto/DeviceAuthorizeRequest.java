package net.hwyz.iov.cloud.sec.ciam.controller.open.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceAuthorizeRequest {
    @NotBlank
    private String clientId;
    private String scope;
}
