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
public class AuthCode {
    private String authCodeId;
    private String clientId;
    private String userId;
    private String sessionId;
    private String codeHash;
    private String redirectUri;
    private String scope;
    private String codeChallenge;
    private String challengeMethod;
    private Instant expireTime;
    private Integer usedFlag;
    private Instant usedTime;
    private String description;
    private Integer rowVersion;
    private Integer rowValid;
    private Instant createTime;
    private Instant modifyTime;
}
