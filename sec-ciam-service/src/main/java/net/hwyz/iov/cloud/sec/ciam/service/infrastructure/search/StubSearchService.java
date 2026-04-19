package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.UserSearchDocument;

import java.time.LocalDateTime;

/**
 * 检索服务 — 桩实现（开发与测试环境使用）。
 * <p>
 * 所有检索方法均返回空结果，不依赖 Elasticsearch。
 */
@Slf4j
@org.springframework.stereotype.Service
public class StubSearchService implements SearchService {

    @Override
    public SearchResult<UserSearchDocument> searchUsers(String query, int page, int size) {
        log.info("[SEARCH-STUB] searchUsers: query={}, page={}, size={}", query, page, size);
        return SearchResult.empty(page, size);
    }

    @Override
    public SearchResult<AuditLogSearchDocument> searchAuditLogs(String userId, String eventType,
                                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                                 int page, int size) {
        log.info("[SEARCH-STUB] searchAuditLogs: userId={}, eventType={}, startTime={}, endTime={}, page={}, size={}",
                userId, eventType, startTime, endTime, page, size);
        return SearchResult.empty(page, size);
    }

    @Override
    public SearchResult<RiskEventSearchDocument> searchRiskEvents(String userId, Integer riskLevel,
                                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                                   int page, int size) {
        log.info("[SEARCH-STUB] searchRiskEvents: userId={}, riskLevel={}, startTime={}, endTime={}, page={}, size={}",
                userId, riskLevel, startTime, endTime, page, size);
        return SearchResult.empty(page, size);
    }
}
