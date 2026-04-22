package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 接入应用领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthClient {
    private String clientId;
    private String clientName;
    private String clientSecretHash;
    private String clientType;
    private String redirectUris;
    private String grantTypes;
    private String scopes;
    private Integer pkceRequired;
    private Integer accessTokenTtl;
    private Integer refreshTokenTtl;
    private Integer clientStatus;
    private String description;
}
