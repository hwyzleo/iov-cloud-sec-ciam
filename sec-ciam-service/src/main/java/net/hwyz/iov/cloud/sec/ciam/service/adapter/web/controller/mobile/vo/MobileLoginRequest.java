package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto2;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileLoginRequest {
    @NotBlank private String mobile;
    private String countryCode;
    @NotBlank private String code;
    private DeviceInfoDto2 deviceInfo;
}
