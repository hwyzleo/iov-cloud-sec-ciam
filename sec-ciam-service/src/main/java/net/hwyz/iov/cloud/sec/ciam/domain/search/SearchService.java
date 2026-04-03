package net.hwyz.iov.cloud.sec.ciam.domain.search;

import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.search.document.UserSearchDocument;

import java.time.LocalDateTime;

/**
 * 检索服务接口。
 * <p>
 * 领域层定义检索抽象，具体实现由基础设施层提供（Elasticsearch 或桩实现）。
 */
public interface SearchService {

    /**
     * 检索用户。
     *
     * @param query 关键词查询条件
     * @param page  页码（从 0 开始）
     * @param size  每页大小
     * @return 用户检索结果
     */
    SearchResult<UserSearchDocument> searchUsers(String query, int page, int size);

    /**
     * 检索审计日志。
     *
     * @param userId    用户 ID（可为 null）
     * @param eventType 事件类型（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime   结束时间（可为 null）
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 审计日志检索结果
     */
    SearchResult<AuditLogSearchDocument> searchAuditLogs(String userId, String eventType,
                                                         LocalDateTime startTime, LocalDateTime endTime,
                                                         int page, int size);

    /**
     * 检索安全事件。
     *
     * @param userId    用户 ID（可为 null）
     * @param riskLevel 风险等级（可为 null）
     * @param startTime 开始时间（可为 null）
     * @param endTime   结束时间（可为 null）
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 安全事件检索结果
     */
    SearchResult<RiskEventSearchDocument> searchRiskEvents(String userId, Integer riskLevel,
                                                           LocalDateTime startTime, LocalDateTime endTime,
                                                           int page, int size);
}
