package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 刷新令牌 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenVo {

    private String refreshTokenId;

    private String userId;

    private String sessionId;

    private String clientId;

    private String tokenFingerprint;

    private String parentTokenId;

    private Integer tokenStatus;

    private Instant issueTime;

    private Instant usedTime;

    private Instant revokeTime;

    private Instant expireTime;

    private String description;
}