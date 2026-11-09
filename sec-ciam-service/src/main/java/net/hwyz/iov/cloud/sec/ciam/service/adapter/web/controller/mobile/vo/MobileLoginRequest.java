package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileLoginRequest {
    @NotBlank private String mobile;
    private String countryCode;
    @NotBlank private String code;

    /**
     * 设备信息（SEC-CIAM-DSN-CR-003 §6.2）：缺失时返回参数校验错误而不是空指针。
     * clientId/clientType/deviceId 由 Controller 以可信边界内的必需 Header 覆盖。
     */
    @Valid
    @NotNull(message = "设备信息不能为空")
    private DeviceInfoDto deviceInfo;
}
