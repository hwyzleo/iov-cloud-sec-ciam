package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

/**
 * 令牌搜索条件领域模型
 */
@Data
@Builder
public class TokenSearchCriteria {
    private String refreshTokenId;
    private String userId;
    private String sessionId;
    private String clientId;
    private Integer tokenStatus;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
