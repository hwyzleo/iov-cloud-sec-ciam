package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeactivationRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserIdentityDTO;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ReviewStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.query.UserQuery;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.search.SearchService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.*;
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
 * AccountQueryAppService 单元测试。
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
            when(userRepository.search(any(UserQuery.class))).thenReturn(List.of(stubUser()));
            when(identityRepository.findByUserId(USER_ID)).thenReturn(List.of(stubIdentity("MOBILE")));
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            List<UserSearchDocument> result = service.queryUserList(UserQuery.builder().build());

            assertEquals(1, result.size());
            assertEquals(USER_ID, result.get(0).getUserId());
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

            List<UserIdentityDTO> result = service.queryBindingRelations(USER_ID);

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
        void returnsAllMergeRequests() {
            List<CiamMergeRequestDo> all = List.of(
                    stubMergeRequest("mr-001"),
                    stubMergeRequest("mr-002"),
                    stubMergeRequest("mr-003"));
            when(mergeRequestRepository.findByReviewStatus(ReviewStatus.PENDING.getCode())).thenReturn(all);

            List<MergeRequestDTO> result = service.queryMergeRequests(ReviewStatus.PENDING.getCode());

            assertEquals(3, result.size());
        }

        @Test
        void returnsEmptyWhenNoRequests() {
            when(mergeRequestRepository.findByReviewStatus(ReviewStatus.APPROVED.getCode()))
                    .thenReturn(Collections.emptyList());

            List<MergeRequestDTO> result = service.queryMergeRequests(ReviewStatus.APPROVED.getCode());

            assertTrue(result.isEmpty());
        }
    }

    // ---- queryDeactivationRequests ----

    @Nested
    class QueryDeactivationRequestsTests {

        @Test
        void returnsAllDeactivationRequests() {
            List<CiamDeactivationRequestDo> all = List.of(
                    stubDeactivationRequest("dr-001"),
                    stubDeactivationRequest("dr-002"));
            when(deactivationRequestRepository.findByReviewStatus(ReviewStatus.PENDING.getCode())).thenReturn(all);

            List<DeactivationRequestDTO> result = service.queryDeactivationRequests(ReviewStatus.PENDING.getCode());

            assertEquals(2, result.size());
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
    }
}
