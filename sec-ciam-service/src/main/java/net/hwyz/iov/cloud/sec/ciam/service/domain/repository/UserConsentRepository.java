package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserConsent;

import java.util.List;
import java.util.Optional;

/**
 * 用户同意记录仓储接口。
 */
public interface UserConsentRepository {

    /** 根据业务 ID 查询 */
    Optional<UserConsent> findByConsentId(String consentId);

    /** 获取用户的所有同意记录 */
    List<UserConsent> findByUserId(String userId);

    /** 获取用户特定类型的同意记录 */
    List<UserConsent> findByUserIdAndConsentType(String userId, String consentType);

    /** 插入同意记录 */
    int insert(UserConsent entity);

    /** 更新同意记录 */
    int updateByConsentId(UserConsent entity);
}

