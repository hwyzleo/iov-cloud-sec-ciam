package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;

/**
 * Refresh Token 重放/轮换竞争专用异常（SEC-CIAM-DSN-CR-003 §5.2，RD-003-6）。
 * <p>
 * 与普通业务异常区分的原因：重放处理路径需要先撤销会话活跃令牌链，
 * 然后抛出本异常并**提交**撤销结果，因此刷新应用事务必须配置
 * {@code noRollbackFor = RefreshTokenReplayException.class}。
 * <p>
 * 对外统一映射为 {@link CiamErrorCode#TOKEN_INVALID}。
 */
public class RefreshTokenReplayException extends BusinessException {

    public RefreshTokenReplayException(String detail) {
        super(CiamErrorCode.TOKEN_INVALID, detail);
    }
}
