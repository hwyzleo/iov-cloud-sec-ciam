package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.UserSearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StubSearchServiceTest {

    private StubSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new StubSearchService();
    }

    @Test
    void searchUsers_returnsEmptyResult() {
        SearchResult<UserSearchDocument> result = searchService.searchUsers("test", 0, 10);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void searchAuditLogs_returnsEmptyResult() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59, 0);

        SearchResult<AuditLogSearchDocument> result =
                searchService.searchAuditLogs("U001", "LOGIN", start, end, 1, 20);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
    }

    @Test
    void searchAuditLogs_withNullParams_returnsEmptyResult() {
        SearchResult<AuditLogSearchDocument> result =
                searchService.searchAuditLogs(null, null, null, null, 0, 10);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
    }

    @Test
    void searchRiskEvents_returnsEmptyResult() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 31, 23, 59, 0);

        SearchResult<RiskEventSearchDocument> result =
                searchService.searchRiskEvents("U001", 2, start, end, 0, 50);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPage());
        assertEquals(50, result.getSize());
    }

    @Test
    void searchRiskEvents_withNullParams_returnsEmptyResult() {
        SearchResult<RiskEventSearchDocument> result =
                searchService.searchRiskEvents(null, null, null, null, 0, 10);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
    }
}
