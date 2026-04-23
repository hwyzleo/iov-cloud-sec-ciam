package net.hwyz.iov.cloud.sec.ciam.service.application.dto.cmd;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto2;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginByMobileCodeCmd {
    @NotBlank(message = "手机号不能为空")
    private String mobile;
    private String countryCode;
    @NotBlank(message = "验证码不能为空")
    private String code;
    @NotBlank(message = "设备标识不能为空")
    private String deviceId;
    @Valid
    @NotNull(message = "设备信息不能为空")
    private DeviceInfoDto2 deviceInfo;
}
