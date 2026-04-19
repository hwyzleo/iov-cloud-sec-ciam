package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ReviewStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamDeactivationRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserTagRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeactivationRequestDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserTagDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.AuditLogSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.RiskEventSearchDocument;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.search.document.UserSearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminQueryAppService 单元测试。
 * <p>
 * 仅 mock 底层仓储与检索服务，与项目现有测试风格一致。
 */
class AccountQueryAppServiceTest {

    private CiamUserRepository userRepository;
    private CiamUserIdentityRepository identityRepository;
    private CiamUserProfileRepository profileRepository;
    private CiamUserTagRepository tagRepository;
    private CiamMergeRequestRepository mergeRequestRepository;
    private CiamDeactivationRequestRepository deactivationRequestRepository;
    private SearchService searchService;
    private FieldEncryptor fieldEncryptor;

    private AccountQueryAppService service;

    private static final String USER_ID = "user-admin-001";

    @BeforeEach
    void setUp() {
        userRepository = mock(CiamUserRepository.class);
        identityRepository = mock(CiamUserIdentityRepository.class);
        profileRepository = mock(CiamUserProfileRepository.class);
        tagRepository = mock(CiamUserTagRepository.class);
        mergeRequestRepository = mock(CiamMergeRequestRepository.class);
        deactivationRequestRepository = mock(CiamDeactivationRequestRepository.class);
        searchService = mock(SearchService.class);
        fieldEncryptor = mock(FieldEncryptor.class);

        service = new AccountQueryAppService(
                userRepository,
                identityRepository,
                profileRepository,
                tagRepository,
                mergeRequestRepository,
                deactivationRequestRepository,
                searchService,
                fieldEncryptor);
    }

    // ---- helpers ----

    private CiamUserDo stubUser() {
        CiamUserDo user = new CiamUserDo();
        user.setUserId(USER_ID);
        user.setUserStatus(UserStatus.ACTIVE.getCode());
        user.setRowValid(1);
        return user;
    }

    private CiamUserIdentityDo stubIdentity(String identityType) {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("id-" + identityType);
        identity.setUserId(USER_ID);
        identity.setIdentityType(identityType);
        identity.setRowValid(1);
        return identity;
    }

    private CiamUserTagDo stubTag(String tagCode) {
        CiamUserTagDo tag = new CiamUserTagDo();
        tag.setTagId("tag-" + tagCode);
        tag.setUserId(USER_ID);
        tag.setTagCode(tagCode);
        tag.setRowValid(1);
        return tag;
    }

    private CiamMergeRequestDo stubMergeRequest(String requestId) {
        CiamMergeRequestDo req = new CiamMergeRequestDo();
        req.setMergeRequestId(requestId);
        req.setSourceUserId("source-001");
        req.setTargetUserId("target-001");
        req.setReviewStatus(ReviewStatus.PENDING.getCode());
        req.setRowValid(1);
        return req;
    }

    private CiamDeactivationRequestDo stubDeactivationRequest(String requestId) {
        CiamDeactivationRequestDo req = new CiamDeactivationRequestDo();
        req.setDeactivationRequestId(requestId);
        req.setUserId(USER_ID);
        req.setReviewStatus(ReviewStatus.PENDING.getCode());
        req.setRowValid(1);
        return req;
    }

    // ---- queryUser ----

    @Nested
    class QueryUserTests {

        @Test
        void returnsUserDetailWithIdentitiesAndTags() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser()));
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(stubIdentity("MOBILE"), stubIdentity("EMAIL")));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(tagRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(stubTag("REAL_NAME")));

            AccountQueryAppService.UserDetail detail = service.queryUser(USER_ID);

            assertEquals(USER_ID, detail.userId());
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());
            assertThrows(BusinessException.class, () -> service.queryUser("nonexistent"));
        }
    }

    // ---- queryUserList ----

    @Nested
    class QueryUserListTests {

        @Test
        void returnsFilteredUsers() {
            when(userRepository.findAll()).thenReturn(List.of(stubUser()));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(stubIdentity("MOBILE")));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            SearchResult<UserSearchDocument> result = service.queryUserList(
                    null, null, null, null, null, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());
            assertEquals(USER_ID, result.getItems().get(0).getUserId());
        }

        @Test
        void filterByUserId() {
            when(userRepository.findAll()).thenReturn(List.of(stubUser()));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(stubIdentity("MOBILE")));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            SearchResult<UserSearchDocument> result = service.queryUserList(
                    "user-admin-001", null, null, null, null, null, null, null, 0, 10);

            assertEquals(1, result.getTotal());

            result = service.queryUserList(
                    "nonexistent", null, null, null, null, null, null, null, 0, 10);

            assertEquals(0, result.getTotal());
        }
    }

    // ---- queryBindingRelations ----

    @Nested
    class QueryBindingRelationsTests {

        @Test
        void returnsIdentitiesForUser() {
            when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubUser()));
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(stubIdentity("MOBILE"), stubIdentity("WECHAT")));

            List<CiamUserIdentityDo> result = service.queryBindingRelations(USER_ID);

            assertEquals(2, result.size());
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());
            assertThrows(BusinessException.class, () -> service.queryBindingRelations("nonexistent"));
        }
    }

    // ---- queryMergeRequests ----

    @Nested
    class QueryMergeRequestsTests {

        @Test
        void returnsPaginatedMergeRequests() {
            List<CiamMergeRequestDo> all = List.of(
                    stubMergeRequest("mr-001"),
                    stubMergeRequest("mr-002"),
                    stubMergeRequest("mr-003"));
            when(mergeRequestRepository.findByReviewStatus(ReviewStatus.PENDING.getCode())).thenReturn(all);

            SearchResult<CiamMergeRequestDo> result = service.queryMergeRequests(
                    ReviewStatus.PENDING.getCode(), 0, 2);

            assertEquals(3, result.getTotal());
            assertEquals(2, result.getItems().size());
            assertEquals(0, result.getPage());
            assertEquals(2, result.getSize());
        }

        @Test
        void returnsEmptyWhenNoRequests() {
            when(mergeRequestRepository.findByReviewStatus(ReviewStatus.APPROVED.getCode()))
                    .thenReturn(Collections.emptyList());

            SearchResult<CiamMergeRequestDo> result = service.queryMergeRequests(
                    ReviewStatus.APPROVED.getCode(), 0, 10);

            assertEquals(0, result.getTotal());
            assertTrue(result.getItems().isEmpty());
        }
    }

    // ---- queryDeactivationRequests ----

    @Nested
    class QueryDeactivationRequestsTests {

        @Test
        void returnsPaginatedDeactivationRequests() {
            List<CiamDeactivationRequestDo> all = List.of(
                    stubDeactivationRequest("dr-001"),
                    stubDeactivationRequest("dr-002"));
            when(deactivationRequestRepository.findByReviewStatus(ReviewStatus.PENDING.getCode())).thenReturn(all);

            SearchResult<CiamDeactivationRequestDo> result = service.queryDeactivationRequests(
                    ReviewStatus.PENDING.getCode(), 0, 10);

            assertEquals(2, result.getTotal());
            assertEquals(2, result.getItems().size());
        }
    }

    // ---- queryAuditLogs ----

    @Nested
    class QueryAuditLogsTests {

        @Test
        void delegatesToSearchService() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59, 0);
            SearchResult<AuditLogSearchDocument> expected = SearchResult.<AuditLogSearchDocument>builder()
                    .items(List.of(AuditLogSearchDocument.builder().auditId("a-001").userId(USER_ID).build()))
                    .total(1).page(0).size(20).build();
            when(searchService.searchAuditLogs(USER_ID, "LOGIN", start, end, 0, 20)).thenReturn(expected);

            SearchResult<AuditLogSearchDocument> result = service.queryAuditLogs(
                    USER_ID, "LOGIN", start, end, 0, 20);

            assertEquals(1, result.getTotal());
            assertEquals("a-001", result.getItems().get(0).getAuditId());
            verify(searchService).searchAuditLogs(USER_ID, "LOGIN", start, end, 0, 20);
        }

        @Test
        void supportsNullFilters() {
            SearchResult<AuditLogSearchDocument> expected = SearchResult.empty(0, 10);
            when(searchService.searchAuditLogs(null, null, null, null, 0, 10)).thenReturn(expected);

            SearchResult<AuditLogSearchDocument> result = service.queryAuditLogs(
                    null, null, null, null, 0, 10);

            assertEquals(0, result.getTotal());
            verify(searchService).searchAuditLogs(null, null, null, null, 0, 10);
        }
    }

    // ---- queryRiskEvents ----

    @Nested
    class QueryRiskEventsTests {

        @Test
        void delegatesToSearchService() {
            LocalDateTime start = LocalDateTime.of(2026, 3, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 3, 31, 23, 59, 0);
            SearchResult<RiskEventSearchDocument> expected = SearchResult.<RiskEventSearchDocument>builder()
                    .items(List.of(RiskEventSearchDocument.builder().riskEventId("r-001").userId(USER_ID).build()))
                    .total(1).page(0).size(20).build();
            when(searchService.searchRiskEvents(USER_ID, 2, start, end, 0, 20)).thenReturn(expected);

            SearchResult<RiskEventSearchDocument> result = service.queryRiskEvents(
                    USER_ID, 2, start, end, 0, 20);

            assertEquals(1, result.getTotal());
            assertEquals("r-001", result.getItems().get(0).getRiskEventId());
            verify(searchService).searchRiskEvents(USER_ID, 2, start, end, 0, 20);
        }

        @Test
        void supportsNullFilters() {
            SearchResult<RiskEventSearchDocument> expected = SearchResult.empty(0, 10);
            when(searchService.searchRiskEvents(null, null, null, null, 0, 10)).thenReturn(expected);

            SearchResult<RiskEventSearchDocument> result = service.queryRiskEvents(
                    null, null, null, null, 0, 10);

            assertEquals(0, result.getTotal());
            verify(searchService).searchRiskEvents(null, null, null, null, 0, 10);
        }
    }
}
