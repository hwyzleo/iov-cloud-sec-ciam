package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.TokenSearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * 刷新令牌仓储接口。
 */
public interface RefreshTokenRepository {

    /** 根据业务 ID 查询 */
    Optional<RefreshToken> findByRefreshTokenId(String refreshTokenId);

    /** 插入令牌记录 */
    int insert(RefreshToken entity);

    /** 根据业务 ID 更新 */
    int updateByRefreshTokenId(RefreshToken entity);

    /** 撤销该会话下的所有活跃令牌 */
    int revokeAllBySessionId(String sessionId);

    /** 撤销该用户下的所有活跃令牌 */
    int revokeAllByUserId(String userId);

    /** 根据令牌指纹查询 */
    Optional<RefreshToken> findByTokenFingerprint(String tokenFingerprint);

    /** 检索令牌列表（支持条件过滤） */
    List<RefreshToken> search(TokenSearchCriteria criteria);

    /** 根据用户 ID 查询 */
    List<RefreshToken> findByUserId(String userId);

    /** 根据会话 ID 查询 */
    List<RefreshToken> findBySessionId(String sessionId);
}
