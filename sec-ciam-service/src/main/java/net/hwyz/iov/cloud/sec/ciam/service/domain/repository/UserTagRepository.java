package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserTag;

import java.util.List;
import java.util.Optional;

/**
 * 认证标签表仓储接口。
 */
public interface UserTagRepository {

    /** 根据用户 ID 和标签编码查询有效标签 */
    Optional<UserTag> findByUserIdAndTagCode(String userId, String tagCode);

    /** 根据用户 ID 查询所有有效标签 */
    List<UserTag> findByUserId(String userId);

    /** 根据业务 ID 查询 */
    Optional<UserTag> findByTagId(String tagId);

    /** 插入标签记录 */
    int insert(UserTag entity);

    /** 根据业务 ID 更新 */
    int updateByTagId(UserTag entity);
}
