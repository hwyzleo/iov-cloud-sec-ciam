package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileLoginRequest {
    @NotBlank private String mobile;
    private String countryCode;
    @NotBlank private String code;
    private DeviceInfoDTO deviceInfo;
}
