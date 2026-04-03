package net.hwyz.iov.cloud.sec.ciam.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话校验结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionValidateResult {

    /** 会话是否有效 */
    private boolean valid;

    /** 会话 ID */
    private String sessionId;

    /** 用户业务唯一标识 */
    private String userId;
}
