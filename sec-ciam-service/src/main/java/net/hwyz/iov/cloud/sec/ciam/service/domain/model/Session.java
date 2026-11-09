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
public class Session {
    private String sessionId;
    private String userId;
    private String deviceId;
    private String clientId;
    private String clientType;
    /** 登录时冻结的授权范围（空格分隔，SEC-CIAM-DSN-CR-003） */
    private String scope;
    private String loginIp;
    private String loginRegion;
    private Integer riskLevel;
    private Integer sessionStatus;
    private Instant loginTime;
    private Instant lastActiveTime;
    private Instant logoutTime;
    private Instant expireTime;
    private Instant modifyTime;
    private String description;
}
