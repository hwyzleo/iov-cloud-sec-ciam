package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

import java.util.List;

/**
 * OIDC Discovery Document DTO。
 * <p>
 * 对应 {@code /.well-known/openid-configuration} 端点返回内容，
 * 包含 issuer、标准端点地址与支持的能力声明。
 */
@Getter
public class OidcDiscoveryDocument {

    private final String issuer;
    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;
    private final String jwksUri;
    private final String deviceAuthorizationEndpoint;
    private final List<String> responseTypesSupported;
    private final List<String> grantTypesSupported;
    private final List<String> subjectTypesSupported;
    private final List<String> idTokenSigningAlgValuesSupported;
    private final List<String> scopesSupported;

    public OidcDiscoveryDocument(String issuer) {
        this.issuer = issuer;
        this.authorizationEndpoint = issuer + "/api/v1/oauth/authorize";
        this.tokenEndpoint = issuer + "/api/v1/oauth/token";
        this.userinfoEndpoint = issuer + "/api/v1/oidc/userinfo";
        this.jwksUri = issuer + "/api/v1/oidc/jwks";
        this.deviceAuthorizationEndpoint = issuer + "/api/v1/oauth/device";
        this.responseTypesSupported = List.of("code");
        this.grantTypesSupported = List.of(
                "authorization_code",
                "client_credentials",
                "urn:ietf:params:oauth:grant-type:device_code",
                "refresh_token");
        this.subjectTypesSupported = List.of("public");
        this.idTokenSigningAlgValuesSupported = List.of("RS256");
        this.scopesSupported = List.of("openid", "profile", "email", "phone");
    }
}
