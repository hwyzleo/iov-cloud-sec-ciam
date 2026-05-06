package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户身份 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityVo {
    
    private String identityId;
    
    private String userId;
    
    private String identityType;
    
    private String identityValue;
    
    private String countryCode;
    
    private Integer verifiedFlag;
    
    private Integer primaryFlag;
    
    private String bindSource;
    
    private Integer identityStatus;
    
    private Instant bindTime;
}