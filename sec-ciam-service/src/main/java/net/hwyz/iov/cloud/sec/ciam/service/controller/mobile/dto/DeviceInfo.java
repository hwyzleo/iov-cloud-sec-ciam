package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    /** 客户端类型 */
    private String clientType;
    /** 客户端业务唯一标识 */
    private String clientId;
    /** 应用版本 */
    private String appVersion;
    /** 设备业务唯一标识 */
    private String deviceId;
    /** 设备名称（如：张三的 iPhone 15） */
    private String deviceName;
    /** 设备操作系统 */
    private String deviceOs;
    /** 设备指纹 */
    private String deviceFingerprint;
    /** 语言 */
    private String language;
}
