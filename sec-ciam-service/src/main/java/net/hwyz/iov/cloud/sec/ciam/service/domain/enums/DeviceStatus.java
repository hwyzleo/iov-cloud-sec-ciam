package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 设备状态枚举。
 * <p>
 * 对应表 {@code ciam_device.device_status}（TINYINT）。
 */
@Getter
public enum DeviceStatus implements CodeEnum {

    INVALID(0, "失效"),
    ACTIVE(1, "正常");

    private final int code;
    private final String description;

    DeviceStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DeviceStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的设备状态编码: " + code));
    }
}
