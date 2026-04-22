package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginByLocalMobileRequest {
    @NotBlank private String token;
    @Valid private DeviceInfoDto deviceInfo;
}
