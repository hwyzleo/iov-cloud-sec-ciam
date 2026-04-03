package net.hwyz.iov.cloud.sec.ciam.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;

import java.util.List;
import java.util.Optional;

/**
 * 登录标识表仓储接口。
 */
public interface CiamUserIdentityRepository {

    /** 根据标识类型和哈希值查询有效记录（唯一性校验） */
    Optional<CiamUserIdentityDo> findByTypeAndHash(String identityType, String identityHash);

    /** 根据用户 ID 查询所有有效标识 */
    List<CiamUserIdentityDo> findByUserId(String userId);

    /** 根据业务 ID 查询 */
    Optional<CiamUserIdentityDo> findByIdentityId(String identityId);

    /** 插入标识记录 */
    int insert(CiamUserIdentityDo entity);

    /** 根据业务 ID 更新 */
    int updateByIdentityId(CiamUserIdentityDo entity);

    /** 物理删除用户所有标识记录（注销场景） */
    int physicalDeleteByUserId(String userId);
}
