package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

/**
 * 用户搜索条件领域模型
 */
@Data
@Builder
public class UserSearchCriteria {
    private String userId;
    private String identityType;
    private String identityValue;
    private String nickname;
    private String registerSource;
    private Integer userStatus;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
