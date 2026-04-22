package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MfaChallengePo;

import java.util.List;
import java.util.Optional;

/**
 * MFA 挑战表仓储接口。
 */
public interface CiamMfaChallengeRepository {

    /** 根据业务 ID 查询 */
    Optional<MfaChallengePo> findByChallengeId(String challengeId);

    /** 根据用户 ID 和挑战状态查询 */
    List<MfaChallengePo> findByUserIdAndStatus(String userId, int challengeStatus);

    /** 插入挑战记录 */
    int insert(MfaChallengePo entity);

    /** 根据业务 ID 更新 */
    int updateByChallengeId(MfaChallengePo entity);
}
