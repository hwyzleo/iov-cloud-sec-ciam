package net.hwyz.iov.cloud.sec.ciam.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Token 校验结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyResult {

    /** 用户业务唯一标识 */
    private String userId;

    /** 客户端标识 */
    private String clientId;

    /** 授权范围 */
    private String scope;

    /** Token 中的声明 */
    private Map<String, Object> claims;
}
