package net.hwyz.iov.cloud.sec.ciam.service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JWK 密钥领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Jwk {
    private String keyId;
    private String privateKeyPem;
    private String publicKeyPem;
    private String algorithm;
    private Integer keySize;
    private Integer status;
    private Instant issueTime;
    private Instant expireTime;
    private Integer isPrimary;
}
