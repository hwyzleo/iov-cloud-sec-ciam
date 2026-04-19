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
public class LoginByLocalMobileRequest {
    @NotBlank
    private String token;
    @NotBlank
    private String clientId;
    private DeviceInfo deviceInfo;
}
