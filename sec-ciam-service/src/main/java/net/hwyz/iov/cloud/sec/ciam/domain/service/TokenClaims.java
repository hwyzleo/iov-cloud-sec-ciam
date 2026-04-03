package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * JWT Access Token 解析后的标准声明集 DTO。
 */
@Getter
@Builder
public class TokenClaims {

    /** 用户业务唯一标识 */
    private final String sub;
    /** OAuth 客户端标识 */
    private final String clientId;
    /** 授权范围 */
    private final String scope;
    /** 会话业务唯一标识 */
    private final String sessionId;
    /** 签发者 */
    private final String iss;
    /** 签发时间 */
    private final Instant iat;
    /** 过期时间 */
    private final Instant exp;
}
