package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;

import java.util.List;
import java.util.Optional;

/**
 * 接入应用表仓储接口。
 */
public interface CiamOAuthClientRepository {

    /** 根据客户端标识查询 */
    Optional<CiamOAuthClientDo> findByClientId(String clientId);

    /** 根据客户端状态查询 */
    List<CiamOAuthClientDo> findByClientStatus(int clientStatus);

    /** 插入客户端记录 */
    int insert(CiamOAuthClientDo entity);

    /** 根据客户端标识更新 */
    int updateByClientId(CiamOAuthClientDo entity);
}
