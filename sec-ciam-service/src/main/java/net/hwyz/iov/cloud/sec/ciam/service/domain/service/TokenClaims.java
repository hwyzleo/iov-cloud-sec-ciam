package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * JWT Access Token 解析后的标准声明集 DTO。
 */
@Getter
@Builder
@AllArgsConstructor
public class TokenClaims {

    /** 用户业务唯一标识 */
    private String sub;
    /** OAuth 客户端标识 */
    private String clientId;
    /** 授权范围 */
    private String scope;
    /** 会话业务唯一标识 */
    private String sessionId;
    /** 签发者 */
    private String iss;
    /** 签发时间 */
    private Instant iat;
    /** 过期时间 */
    private Instant exp;
}
