package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserDo;

import java.util.List;
import java.util.Optional;

/**
 * 用户主表仓储接口。
 */
public interface CiamUserRepository {

    /** 根据业务 ID 查询用户 */
    Optional<CiamUserDo> findByUserId(String userId);

    /** 查询所有用户 */
    List<CiamUserDo> findAll();

    /** 根据用户状态查询列表 */
    List<CiamUserDo> findByUserStatus(int userStatus);

    /** 插入用户记录 */
    int insert(CiamUserDo entity);

    /** 根据业务 ID 更新用户记录 */
    int updateByUserId(CiamUserDo entity);

    /** 物理删除用户主档记录（注销场景） */
    int physicalDeleteByUserId(String userId);
}
