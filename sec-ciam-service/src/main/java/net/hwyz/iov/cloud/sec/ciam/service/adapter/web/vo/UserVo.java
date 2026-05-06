package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVo {

    private String userId;

    private Integer userStatus;

    private String brandCode;

    private String registerSource;

    private String registerChannel;

    private String primaryIdentityType;

    private Instant lastLoginTime;

    private Instant deactivatedTime;

    private String description;
}