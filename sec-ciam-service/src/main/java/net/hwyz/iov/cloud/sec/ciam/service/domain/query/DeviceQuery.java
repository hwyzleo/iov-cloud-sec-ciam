package net.hwyz.iov.cloud.sec.ciam.service.domain.query;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class DeviceQuery {
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
