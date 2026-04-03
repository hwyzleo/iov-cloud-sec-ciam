package net.hwyz.iov.cloud.sec.ciam.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserProfileDo;

import java.util.Optional;

/**
 * 用户资料扩展表仓储接口。
 */
public interface CiamUserProfileRepository {

    /** 根据用户 ID 查询有效资料 */
    Optional<CiamUserProfileDo> findByUserId(String userId);

    /** 根据业务 ID 查询 */
    Optional<CiamUserProfileDo> findByProfileId(String profileId);

    /** 插入资料记录 */
    int insert(CiamUserProfileDo entity);

    /** 根据业务 ID 更新 */
    int updateByProfileId(CiamUserProfileDo entity);

    /** 物理删除用户资料记录（注销场景） */
    int physicalDeleteByUserId(String userId);
}
