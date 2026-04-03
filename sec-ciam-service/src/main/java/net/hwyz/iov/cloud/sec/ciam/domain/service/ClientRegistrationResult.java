package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

/**
 * OAuth 客户端注册结果。
 * <p>
 * 包含客户端标识、原始密钥（仅注册时返回一次）和客户端名称。
 * 对于公开客户端，{@code clientSecret} 为 {@code null}。
 */
@Getter
public class ClientRegistrationResult {

    private final String clientId;
    private final String clientSecret;
    private final String clientName;

    public ClientRegistrationResult(String clientId, String clientSecret, String clientName) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientName = clientName;
    }
}
