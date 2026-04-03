package net.hwyz.iov.cloud.sec.ciam.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;

import java.util.List;
import java.util.Optional;

/**
 * 刷新令牌表仓储接口。
 */
public interface CiamRefreshTokenRepository {

    /** 根据令牌指纹查询 */
    Optional<CiamRefreshTokenDo> findByTokenFingerprint(String tokenFingerprint);

    /** 根据业务 ID 查询 */
    Optional<CiamRefreshTokenDo> findByRefreshTokenId(String refreshTokenId);

    /** 根据用户 ID 和令牌状态查询 */
    List<CiamRefreshTokenDo> findByUserIdAndStatus(String userId, int tokenStatus);

    /** 根据会话 ID 和令牌状态查询 */
    List<CiamRefreshTokenDo> findBySessionIdAndStatus(String sessionId, int tokenStatus);

    /** 插入令牌记录 */
    int insert(CiamRefreshTokenDo entity);

    /** 根据业务 ID 更新 */
    int updateByRefreshTokenId(CiamRefreshTokenDo entity);

    /** 批量撤销用户所有有效令牌 */
    int revokeAllByUserId(String userId);

    /** 批量撤销指定会话的所有有效令牌 */
    int revokeAllBySessionId(String sessionId);
}
