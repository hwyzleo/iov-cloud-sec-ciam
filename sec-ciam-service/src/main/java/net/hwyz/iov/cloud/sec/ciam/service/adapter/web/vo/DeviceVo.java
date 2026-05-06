package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceVo {

    private String deviceId;

    private String userId;

    private String clientId;

    private String clientType;

    private String deviceName;

    private String deviceModel;

    private String osName;

    private String osVersion;

    private String appVersion;

    private String language;

    private String timezone;

    private Integer deviceStatus;

    private Instant lastLoginTime;

    private Instant createTime;
}