package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RiskEventPo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 风险事件表仓储接口。
 */
public interface CiamRiskEventRepository {

    /** 根据业务 ID 查询 */
    Optional<RiskEventPo> findByRiskEventId(String riskEventId);

    /** 根据用户 ID 和时间范围查询 */
    List<RiskEventPo> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /** 根据风险等级查询 */
    List<RiskEventPo> findByRiskLevel(int riskLevel);

    /** 插入风险事件记录 */
    int insert(RiskEventPo entity);

    /** 根据业务 ID 更新 */
    int updateByRiskEventId(RiskEventPo entity);
}
