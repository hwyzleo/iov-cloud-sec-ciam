package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentity {
    private String identityId;
    private String userId;
    private String identityType;
    private String identityValue;
    private String identityHash;
    private String countryCode;
    private Integer verifiedFlag;
    private Integer primaryFlag;
    private String bindSource;
    private Instant bindTime;
    private Instant unbindTime;
    private Integer identityStatus;
    private String description;
    private Integer rowVersion;
    private Integer rowValid;
    private Instant createTime;
    private Instant modifyTime;
}
