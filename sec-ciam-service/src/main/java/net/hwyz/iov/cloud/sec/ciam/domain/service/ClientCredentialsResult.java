package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

/**
 * Client Credentials 授权结果。
 * <p>
 * 客户端凭据授权成功后返回客户端标识、授权范围和访问令牌有效期，
 * 供后续 Token 签发流程使用。
 */
@Getter
public class ClientCredentialsResult {

    private final String clientId;
    private final String scope;
    private final int accessTokenTtl;

    public ClientCredentialsResult(String clientId, String scope, int accessTokenTtl) {
        this.clientId = clientId;
        this.scope = scope;
        this.accessTokenTtl = accessTokenTtl;
    }
}
