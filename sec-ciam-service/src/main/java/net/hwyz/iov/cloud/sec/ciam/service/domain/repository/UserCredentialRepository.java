package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserCredential;

import java.util.Optional;

/**
 * 用户凭据（密码）仓储接口。
 */
public interface UserCredentialRepository {

    /** 根据用户 ID 和凭据类型查询有效凭据 */
    Optional<UserCredential> findByUserIdAndType(String userId, String credentialType);

    /** 根据业务 ID 查询 */
    Optional<UserCredential> findByCredentialId(String credentialId);

    /** 插入凭据记录 */
    int insert(UserCredential entity);

    /** 更新凭据信息 */
    int updateByCredentialId(UserCredential entity);

    /** 物理删除用户所有凭据记录（注销场景） */
    int physicalDeleteByUserId(String userId);
}
