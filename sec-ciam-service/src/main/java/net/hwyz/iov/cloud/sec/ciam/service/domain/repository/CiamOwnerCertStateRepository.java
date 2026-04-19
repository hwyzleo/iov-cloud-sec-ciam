package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;

import java.util.List;
import java.util.Optional;

/**
 * 车主认证状态表仓储接口。
 */
public interface CiamOwnerCertStateRepository {

    /** 根据业务 ID 查询 */
    Optional<CiamOwnerCertStateDo> findByOwnerCertId(String ownerCertId);

    /** 根据用户 ID 和认证状态查询 */
    List<CiamOwnerCertStateDo> findByUserIdAndCertStatus(String userId, int certStatus);

    /** 根据用户 ID 查询所有认证记录 */
    List<CiamOwnerCertStateDo> findByUserId(String userId);

    /** 插入认证状态记录 */
    int insert(CiamOwnerCertStateDo entity);

    /** 根据业务 ID 更新 */
    int updateByOwnerCertId(CiamOwnerCertStateDo entity);
}
