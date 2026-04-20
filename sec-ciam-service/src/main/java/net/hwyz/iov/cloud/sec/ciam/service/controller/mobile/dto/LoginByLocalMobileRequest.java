package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginByLocalMobileRequest {
    @NotBlank private String token;
    @Valid private DeviceInfo deviceInfo;
}
