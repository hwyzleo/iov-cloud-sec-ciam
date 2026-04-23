package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 风险事件仓储接口。
 */
public interface RiskEventRepository {

    /** 根据业务 ID 查询 */
    Optional<RiskEvent> findByRiskEventId(String riskEventId);

    /** 按用户和时间范围查询风险事件 */
    List<RiskEvent> findByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /** 按风险等级查询 */
    List<RiskEvent> findByRiskLevel(int riskLevel);

    /** 插入风险事件 */
    int insert(RiskEvent entity);

    /** 更新风险事件处置状态 */
    int updateByRiskEventId(RiskEvent entity);
}

