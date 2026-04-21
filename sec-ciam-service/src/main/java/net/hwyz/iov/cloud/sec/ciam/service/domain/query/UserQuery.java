package net.hwyz.iov.cloud.sec.ciam.service.domain.query;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class UserQuery {
    private String userId;
    private String identityType;
    private String identityValue;
    private String nickname;
    private String registerSource;
    private Integer userStatus;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
