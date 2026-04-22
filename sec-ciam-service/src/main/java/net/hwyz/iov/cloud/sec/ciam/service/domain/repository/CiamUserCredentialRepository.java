package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.UserCredentialPo;

import java.util.Optional;

/**
 * 凭据表仓储接口。
 */
public interface CiamUserCredentialRepository {

    /** 根据用户 ID 和凭据类型查询有效凭据 */
    Optional<UserCredentialPo> findByUserIdAndType(String userId, String credentialType);

    /** 根据业务 ID 查询 */
    Optional<UserCredentialPo> findByCredentialId(String credentialId);

    /** 插入凭据记录 */
    int insert(UserCredentialPo entity);

    /** 根据业务 ID 更新 */
    int updateByCredentialId(UserCredentialPo entity);

    /** 物理删除用户所有凭据记录（注销场景） */
    int physicalDeleteByUserId(String userId);
}
