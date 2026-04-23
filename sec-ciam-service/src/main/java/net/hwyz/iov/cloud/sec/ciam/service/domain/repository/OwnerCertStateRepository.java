package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.OwnerCertState;

import java.util.List;
import java.util.Optional;

/**
 * 车主认证状态仓储接口。
 */
public interface OwnerCertStateRepository {

    /** 根据业务 ID 查询 */
    Optional<OwnerCertState> findByOwnerCertId(String ownerCertId);

    /** 根据用户 ID 和认证状态查询 */
    List<OwnerCertState> findByUserIdAndCertStatus(String userId, int certStatus);

    /** 根据用户 ID 查询 */
    List<OwnerCertState> findByUserId(String userId);

    /** 插入认证记录 */
    int insert(OwnerCertState entity);

    /** 根据业务 ID 更新 */
    int updateByOwnerCertId(OwnerCertState entity);
}

