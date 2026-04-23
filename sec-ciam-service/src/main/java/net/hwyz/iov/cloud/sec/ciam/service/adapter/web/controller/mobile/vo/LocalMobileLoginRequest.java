package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

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
public class LocalMobileLoginRequest {
    @NotBlank private String token;
    private DeviceInfoDto deviceInfo;
}
