package net.hwyz.iov.cloud.sec.ciam.service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityDto {
    private String identityId;
    private String userId;
    private String identityType;
    private String identityValue;
    private String countryCode;
    private Integer verifiedFlag;
    private Integer primaryFlag;
    private String bindSource;
    private Instant bindTime;
    private Integer identityStatus;
}
