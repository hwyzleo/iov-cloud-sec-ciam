package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserIdentityPo;

import java.util.List;
import java.util.Optional;

/**
 * 登录标识表仓储接口。
 */
public interface CiamUserIdentityRepository {

    /** 根据标识类型和哈希值查询有效记录（唯一性校验） */
    Optional<UserIdentityPo> findByTypeAndHash(String identityType, String identityHash);

    /** 根据标识类型和原始值查询有效记录 */
    Optional<UserIdentityPo> findByTypeAndValue(String identityType, String identityValue);

    /** 根据用户 ID 查询所有有效标识 */
    List<UserIdentityPo> findByUserId(String userId);

    /** 根据业务 ID 查询 */
    Optional<UserIdentityPo> findByIdentityId(String identityId);

    /** 插入标识记录 */
    int insert(UserIdentityPo entity);

    /** 根据业务 ID 更新 */
    int updateByIdentityId(UserIdentityPo entity);

    /** 更新用户标识值 */
    int updateIdentityValue(String userId, String identityType, String identityHash);

    /** 更新用户标识值（哈希和加密值） */
    int updateIdentityValue(String userId, String identityType, String identityHash, String identityValue);

    /** 物理删除用户所有标识记录（注销场景） */
    int physicalDeleteByUserId(String userId);
}
