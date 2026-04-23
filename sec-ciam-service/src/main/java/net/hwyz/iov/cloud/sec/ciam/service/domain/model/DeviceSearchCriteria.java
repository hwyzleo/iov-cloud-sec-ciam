package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

/**
 * 设备搜索条件领域模型
 */
@Data
@Builder
public class DeviceSearchCriteria {
    private String deviceId;
    private String userId;
    private String clientType;
    private String clientId;
    private String deviceName;
    private String deviceOs;
    private Integer deviceStatus;
    private Boolean trustedFlag;
    private String language;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
