package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserConsentPo;

import java.util.List;
import java.util.Optional;

/**
 * 协议与营销同意表仓储接口。
 */
public interface CiamUserConsentRepository {

    /** 根据业务 ID 查询 */
    Optional<UserConsentPo> findByConsentId(String consentId);

    /** 根据用户 ID 查询所有有效同意记录 */
    List<UserConsentPo> findByUserId(String userId);

    /** 根据用户 ID 和同意类型查询 */
    List<UserConsentPo> findByUserIdAndConsentType(String userId, String consentType);

    /** 插入同意记录 */
    int insert(UserConsentPo entity);

    /** 根据业务 ID 更新 */
    int updateByConsentId(UserConsentPo entity);
}
