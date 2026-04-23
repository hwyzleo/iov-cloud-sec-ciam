package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OAuthClient;

import java.util.List;
import java.util.Optional;

/**
 * 接入应用（OAuth Client）仓储接口。
 */
public interface OAuthClientRepository {

    /** 根据客户端 ID 查询 */
    Optional<OAuthClient> findByClientId(String clientId);

    /** 根据状态查询列表 */
    List<OAuthClient> findByClientStatus(int clientStatus);

    /** 插入客户端记录 */
    int insert(OAuthClient entity);

    /** 根据业务 ID 更新 */
    int updateByClientId(OAuthClient entity);
}

