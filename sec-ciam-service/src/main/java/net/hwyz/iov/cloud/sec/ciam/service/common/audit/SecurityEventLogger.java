package net.hwyz.iov.cloud.sec.ciam.service.common.audit;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RiskEvent;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RiskEventRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 安全事件日志记录器。
 * <p>
 * 独立于审计日志，专门记录 MFA 触发、异常登录、验证码防刷、高风险处置等安全事件，
 * 与 design.md 可观测性设计中的安全事件日志对齐。
 * <p>
 * 安全事件同时持久化到 ciam_risk_event 表和 SLF4J 日志。
 * 若数据库写入失败，降级为仅 SLF4J 记录，保证安全事件链路不中断业务流程。
 */
@Slf4j(topic = "SECURITY")
@Component
public class SecurityEventLogger {

    @Nullable
    private final RiskEventRepository riskEventRepository;

    public SecurityEventLogger(@Nullable RiskEventRepository riskEventRepository) {
        this.riskEventRepository = riskEventRepository;
    }

    /**
     * 无仓储构造函数，仅 SLF4J 记录（向后兼容）。
     */
    public SecurityEventLogger() {
        this.riskEventRepository = null;
    }

    /**
     * 记录安全事件（简单接口，向后兼容）。
     *
     * @param eventType 事件类型
     * @param userId    用户 ID（可为 null）
     * @param ip        请求 IP
     * @param traceId   追踪 ID
     * @param detail    事件详情
     */
    public void log(String eventType, String userId, String ip, String traceId, String detail) {
        log(SecurityEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .ipAddress(ip)
                .traceId(traceId)
                .detail(detail)
                .build());
    }

    /**
     * 记录安全事件（完整接口）。
     * <p>
     * 先写 SLF4J 日志，再尝试持久化到 ciam_risk_event 表。
     * 若数据库写入失败，降级为仅日志记录。
     *
     * @param event 安全事件
     */
    public void log(SecurityEvent event) {
        if (event == null) {
            return;
        }

        // 始终先写 SLF4J 日志
        logToSlf4j(event);

        // 持久化到数据库
        if (riskEventRepository != null) {
            try {
                RiskEvent riskEvent = mapToDomainModel(event);
                riskEventRepository.insert(riskEvent);
            } catch (Exception e) {
                log.warn("[SECURITY] 安全事件持久化失败，已降级为仅日志记录: {}", e.getMessage());
            }
        }
    }

    /**
     * 将 SecurityEvent 映射为 RiskEvent。
     */
    RiskEvent mapToDomainModel(SecurityEvent event) {
        Instant now = DateTimeUtil.getNowInstant();
        return RiskEvent.builder()
                .riskEventId(generateRiskEventId())
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .deviceId(event.getDeviceId())
                .riskScene(event.getRiskScene() != null ? event.getRiskScene() : event.getEventType())
                .riskType(event.getEventType())
                .riskLevel(event.getRiskLevel() != null ? event.getRiskLevel() : 0)
                .clientType(event.getClientType())
                .ipAddress(event.getIpAddress())
                .regionCode(event.getRegionCode())
                .decisionResult(event.getDecisionResult() != null ? event.getDecisionResult() : "log")
                .hitRules(event.getHitRules())
                .eventTime(event.getEventTime() != null ? event.getEventTime() : now)
                .handledFlag(0)
                .description(event.getDetail())
                .build();
    }

    private void logToSlf4j(SecurityEvent event) {
        log.warn("[SECURITY] type={} userId={} ip={} traceId={} detail={}",
                event.getEventType(), event.getUserId(), event.getIpAddress(),
                event.getTraceId(), event.getDetail());
    }

    private String generateRiskEventId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
