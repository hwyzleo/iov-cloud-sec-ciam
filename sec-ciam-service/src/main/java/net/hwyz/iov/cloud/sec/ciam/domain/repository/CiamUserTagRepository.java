package net.hwyz.iov.cloud.sec.ciam.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserTagDo;

import java.util.List;
import java.util.Optional;

/**
 * 认证标签表仓储接口。
 */
public interface CiamUserTagRepository {

    /** 根据用户 ID 和标签编码查询有效标签 */
    Optional<CiamUserTagDo> findByUserIdAndTagCode(String userId, String tagCode);

    /** 根据用户 ID 查询所有有效标签 */
    List<CiamUserTagDo> findByUserId(String userId);

    /** 根据业务 ID 查询 */
    Optional<CiamUserTagDo> findByTagId(String tagId);

    /** 插入标签记录 */
    int insert(CiamUserTagDo entity);

    /** 根据业务 ID 更新 */
    int updateByTagId(CiamUserTagDo entity);
}
