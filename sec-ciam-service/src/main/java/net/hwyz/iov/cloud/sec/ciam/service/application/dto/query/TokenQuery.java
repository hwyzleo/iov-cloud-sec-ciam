package net.hwyz.iov.cloud.sec.ciam.service.application.dto.query;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class TokenQuery {
    private String refreshTokenId;
    private String userId;
    private String sessionId;
    private String clientId;
    private Integer tokenStatus;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
