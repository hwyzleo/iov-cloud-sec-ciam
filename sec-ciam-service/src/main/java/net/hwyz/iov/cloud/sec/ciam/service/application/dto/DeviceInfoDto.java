package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoDto {
    private String clientType;
    private String clientId;
    private String appVersion;
    private String deviceId;
    private String deviceName;
    private String deviceOs;
    private String deviceFingerprint;
    private String language;
}
