package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRefreshTokenDo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.TokenQuery;

import java.util.List;
import java.util.Optional;

/**
 * 刷新令牌仓储接口。
 */
public interface CiamRefreshTokenRepository {

    /** 根据业务 ID 查询 */
    Optional<CiamRefreshTokenDo> findByRefreshTokenId(String refreshTokenId);

    /** 插入令牌记录 */
    int insert(CiamRefreshTokenDo entity);

    /** 根据业务 ID 更新 */
    int updateByRefreshTokenId(CiamRefreshTokenDo entity);

    /** 撤销该会话下的所有活跃令牌 */
    int revokeAllBySessionId(String sessionId);

    /** 撤销该用户下的所有活跃令牌 */
    int revokeAllByUserId(String userId);

    /** 根据令牌指纹查询 */
    Optional<CiamRefreshTokenDo> findByTokenFingerprint(String tokenFingerprint);

    /** 查询所有令牌 */
    List<CiamRefreshTokenDo> findAll();

    /** 检索令牌列表（支持条件过滤） */
    List<CiamRefreshTokenDo> search(TokenQuery query);

    /** 根据用户 ID 查询 */
    List<CiamRefreshTokenDo> findByUserId(String userId);

    /** 根据会话 ID 查询 */
    List<CiamRefreshTokenDo> findBySessionId(String sessionId);
}
