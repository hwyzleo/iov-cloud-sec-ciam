package net.hwyz.iov.cloud.sec.ciam.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.ReviewStatus;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AccountBindingAppService 单元测试。
 * <p>
 * 构造真实的 IdentityDomainService 实例，仅 mock 底层仓储接口，
 * 与项目现有测试风格保持一致。
 */
class AccountBindingAppServiceTest {

    private CiamUserIdentityRepository identityRepository;
    private CiamMergeRequestRepository mergeRequestRepository;
    private AuditLogger auditLogger;
    private FieldEncryptor fieldEncryptor;

    private IdentityDomainService identityDomainService;
    private AccountBindingAppService service;

    private static final String USER_ID = "user-001";
    private static final String OTHER_USER_ID = "user-002";
    private static final String PHONE = "13800138000";
    private static final String PHONE_HASH = FieldEncryptor.hash(PHONE);
    private static final String COUNTRY_CODE = "+86";
    private static final String BIND_SOURCE = "app";

    // Generate a valid 32-byte AES key for FieldEncryptor
    private static final String AES_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @BeforeEach
    void setUp() {
        identityRepository = mock(CiamUserIdentityRepository.class);
        mergeRequestRepository = mock(CiamMergeRequestRepository.class);
        auditLogger = mock(AuditLogger.class);
        fieldEncryptor = new FieldEncryptor(AES_KEY);

        when(identityRepository.insert(any())).thenReturn(1);
        when(identityRepository.updateByIdentityId(any())).thenReturn(1);
        when(mergeRequestRepository.insert(any())).thenReturn(1);
        when(mergeRequestRepository.updateByMergeRequestId(any())).thenReturn(1);

        identityDomainService = new IdentityDomainService(identityRepository, fieldEncryptor);

        service = new AccountBindingAppService(
                identityDomainService, mergeRequestRepository, fieldEncryptor, auditLogger);
    }

    private CiamUserIdentityDo stubBoundIdentity(String userId, IdentityType type, String rawValue) {
        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId("identity-" + rawValue.hashCode());
        identity.setUserId(userId);
        identity.setIdentityType(type.getCode());
        identity.setIdentityValue(fieldEncryptor.encrypt(rawValue));
        identity.setIdentityHash(FieldEncryptor.hash(rawValue));
        identity.setCountryCode(COUNTRY_CODE);
        identity.setIdentityStatus(IdentityStatus.BOUND.getCode());
        identity.setRowValid(1);
        return identity;
    }

    private CiamMergeRequestDo stubMergeRequest(String mergeRequestId, int reviewStatus) {
        CiamMergeRequestDo request = new CiamMergeRequestDo();
        request.setMergeRequestId(mergeRequestId);
        request.setSourceUserId(USER_ID);
        request.setTargetUserId(OTHER_USER_ID);
        request.setConflictIdentityType(IdentityType.MOBILE.getCode());
        request.setConflictIdentityHash(PHONE_HASH);
        request.setReviewStatus(reviewStatus);
        request.setRowValid(1);
        request.setRowVersion(1);
        return request;
    }

    // ========== Task 10.1: bindIdentity ==========

    @Nested
    class BindIdentityTests {

        @Test
        void bindsIdentitySuccessfullyWhenNoConflict() {
            // No existing identity with this hash
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), PHONE_HASH))
                    .thenReturn(Optional.empty());

            CiamUserIdentityDo result = service.bindIdentity(
                    USER_ID, IdentityType.MOBILE, PHONE, COUNTRY_CODE, BIND_SOURCE);

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(IdentityType.MOBILE.getCode(), result.getIdentityType());
            assertEquals(IdentityStatus.BOUND.getCode(), result.getIdentityStatus());

            // Verify audit log
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("BIND", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void createsmergeRequestAndThrowsWhenConflictDetected() {
            // Identity already bound to another user
            CiamUserIdentityDo conflictIdentity = stubBoundIdentity(OTHER_USER_ID, IdentityType.MOBILE, PHONE);
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), PHONE_HASH))
                    .thenReturn(Optional.of(conflictIdentity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.bindIdentity(USER_ID, IdentityType.MOBILE, PHONE, COUNTRY_CODE, BIND_SOURCE));

            assertEquals(CiamErrorCode.MERGE_REQUEST_PENDING, ex.getErrorCode());

            // Verify merge request was created
            ArgumentCaptor<CiamMergeRequestDo> mrCaptor = ArgumentCaptor.forClass(CiamMergeRequestDo.class);
            verify(mergeRequestRepository).insert(mrCaptor.capture());
            CiamMergeRequestDo mr = mrCaptor.getValue();
            assertEquals(USER_ID, mr.getSourceUserId());
            assertEquals(OTHER_USER_ID, mr.getTargetUserId());
            assertEquals(ReviewStatus.PENDING.getCode(), mr.getReviewStatus());
        }

        @Test
        void returnsSameIdentityWhenAlreadyBoundToSameUser() {
            // Identity already bound to the same user
            CiamUserIdentityDo existing = stubBoundIdentity(USER_ID, IdentityType.MOBILE, PHONE);
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), PHONE_HASH))
                    .thenReturn(Optional.of(existing));

            CiamUserIdentityDo result = service.bindIdentity(
                    USER_ID, IdentityType.MOBILE, PHONE, COUNTRY_CODE, BIND_SOURCE);

            // Should return existing identity without creating a new one
            assertEquals(existing.getIdentityId(), result.getIdentityId());
            // No merge request should be created
            verify(mergeRequestRepository, never()).insert(any());
        }
    }

    // ========== Task 10.2: unbindIdentity ==========

    @Nested
    class UnbindIdentityTests {

        @Test
        void unbindsIdentityWhenMultipleBound() {
            // User has 2 bound identities
            CiamUserIdentityDo identity1 = stubBoundIdentity(USER_ID, IdentityType.MOBILE, PHONE);
            CiamUserIdentityDo identity2 = stubBoundIdentity(USER_ID, IdentityType.EMAIL, "test@example.com");
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(identity1, identity2));
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), PHONE_HASH))
                    .thenReturn(Optional.of(identity1));

            service.unbindIdentity(USER_ID, IdentityType.MOBILE, PHONE_HASH);

            verify(identityRepository).updateByIdentityId(any());

            // Verify audit log
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("UNBIND", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void throwsWhenUnbindingLastIdentity() {
            // User has only 1 bound identity
            CiamUserIdentityDo identity = stubBoundIdentity(USER_ID, IdentityType.MOBILE, PHONE);
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(List.of(identity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.unbindIdentity(USER_ID, IdentityType.MOBILE, PHONE_HASH));

            assertEquals(CiamErrorCode.UNBIND_LAST_IDENTITY, ex.getErrorCode());
            // Should not call unbind on domain service
            verify(identityRepository, never()).updateByIdentityId(any());
            verify(auditLogger, never()).log(any());
        }

        @Test
        void throwsWhenNoBoundIdentities() {
            when(identityRepository.findByUserId(USER_ID))
                    .thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.unbindIdentity(USER_ID, IdentityType.MOBILE, PHONE_HASH));

            assertEquals(CiamErrorCode.UNBIND_LAST_IDENTITY, ex.getErrorCode());
        }
    }

    // ========== Task 10.3: createMergeRequest ==========

    @Nested
    class CreateMergeRequestTests {

        @Test
        void createsMergeRequestWithPendingStatus() {
            CiamMergeRequestDo result = service.createMergeRequest(
                    USER_ID, OTHER_USER_ID,
                    IdentityType.MOBILE.getCode(), PHONE_HASH, BIND_SOURCE);

            assertNotNull(result);
            assertNotNull(result.getMergeRequestId());
            assertEquals(USER_ID, result.getSourceUserId());
            assertEquals(OTHER_USER_ID, result.getTargetUserId());
            assertEquals(IdentityType.MOBILE.getCode(), result.getConflictIdentityType());
            assertEquals(PHONE_HASH, result.getConflictIdentityHash());
            assertEquals(ReviewStatus.PENDING.getCode(), result.getReviewStatus());
            assertEquals(1, result.getRowValid());

            verify(mergeRequestRepository).insert(any(CiamMergeRequestDo.class));

            // Verify audit log
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("MERGE", captor.getValue().getEventType());
            assertEquals("合并申请", captor.getValue().getEventName());
        }
    }

    // ========== Task 10.4: approveMergeRequest & executeMerge ==========

    @Nested
    class ApproveMergeRequestTests {

        @Test
        void approvesMergeRequestSuccessfully() {
            String mergeRequestId = "mr-001";
            CiamMergeRequestDo request = stubMergeRequest(mergeRequestId, ReviewStatus.PENDING.getCode());
            when(mergeRequestRepository.findByMergeRequestId(mergeRequestId))
                    .thenReturn(Optional.of(request));

            service.approveMergeRequest(mergeRequestId, "admin-001");

            ArgumentCaptor<CiamMergeRequestDo> captor = ArgumentCaptor.forClass(CiamMergeRequestDo.class);
            verify(mergeRequestRepository).updateByMergeRequestId(captor.capture());
            CiamMergeRequestDo updated = captor.getValue();
            assertEquals(ReviewStatus.APPROVED.getCode(), updated.getReviewStatus());
            assertEquals("admin-001", updated.getReviewer());
            assertNotNull(updated.getReviewTime());
        }

        @Test
        void throwsWhenMergeRequestNotFound() {
            when(mergeRequestRepository.findByMergeRequestId("nonexistent"))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.approveMergeRequest("nonexistent", "admin-001"));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }

    @Nested
    class ExecuteMergeTests {

        @Test
        void executeMergeSuccessfully() {
            String mergeRequestId = "mr-001";
            CiamMergeRequestDo request = stubMergeRequest(mergeRequestId, ReviewStatus.APPROVED.getCode());
            when(mergeRequestRepository.findByMergeRequestId(mergeRequestId))
                    .thenReturn(Optional.of(request));

            // Non-final user (OTHER_USER_ID) has one identity to migrate
            String email = "other@example.com";
            CiamUserIdentityDo otherIdentity = stubBoundIdentity(OTHER_USER_ID, IdentityType.EMAIL, email);
            when(identityRepository.findByUserId(OTHER_USER_ID))
                    .thenReturn(List.of(otherIdentity));

            // For unbind: find the identity by type and hash
            when(identityRepository.findByTypeAndHash(
                    IdentityType.EMAIL.getCode(), FieldEncryptor.hash(email)))
                    .thenReturn(Optional.of(otherIdentity))
                    // After unbind, next call for bind should return empty (no conflict)
                    .thenReturn(Optional.empty());

            service.executeMerge(mergeRequestId, USER_ID);

            // Verify merge request was updated with final user and finish time
            ArgumentCaptor<CiamMergeRequestDo> mrCaptor = ArgumentCaptor.forClass(CiamMergeRequestDo.class);
            verify(mergeRequestRepository).updateByMergeRequestId(mrCaptor.capture());
            CiamMergeRequestDo updated = mrCaptor.getValue();
            assertEquals(USER_ID, updated.getFinalUserId());
            assertNotNull(updated.getFinishTime());

            // Verify audit log for merge complete
            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("MERGE", auditCaptor.getValue().getEventType());
            assertEquals("合并完成", auditCaptor.getValue().getEventName());
        }

        @Test
        void throwsWhenMergeRequestNotApproved() {
            String mergeRequestId = "mr-001";
            CiamMergeRequestDo request = stubMergeRequest(mergeRequestId, ReviewStatus.PENDING.getCode());
            when(mergeRequestRepository.findByMergeRequestId(mergeRequestId))
                    .thenReturn(Optional.of(request));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.executeMerge(mergeRequestId, USER_ID));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenFinalUserNotInMergeRequest() {
            String mergeRequestId = "mr-001";
            CiamMergeRequestDo request = stubMergeRequest(mergeRequestId, ReviewStatus.APPROVED.getCode());
            when(mergeRequestRepository.findByMergeRequestId(mergeRequestId))
                    .thenReturn(Optional.of(request));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.executeMerge(mergeRequestId, "user-999"));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenMergeRequestNotFound() {
            when(mergeRequestRepository.findByMergeRequestId("nonexistent"))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.executeMerge("nonexistent", USER_ID));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void executeMergeWithNoIdentitiesToMigrate() {
            String mergeRequestId = "mr-001";
            CiamMergeRequestDo request = stubMergeRequest(mergeRequestId, ReviewStatus.APPROVED.getCode());
            when(mergeRequestRepository.findByMergeRequestId(mergeRequestId))
                    .thenReturn(Optional.of(request));

            // Non-final user has no bound identities
            when(identityRepository.findByUserId(OTHER_USER_ID))
                    .thenReturn(Collections.emptyList());

            service.executeMerge(mergeRequestId, USER_ID);

            // Should still mark as complete
            ArgumentCaptor<CiamMergeRequestDo> mrCaptor = ArgumentCaptor.forClass(CiamMergeRequestDo.class);
            verify(mergeRequestRepository).updateByMergeRequestId(mrCaptor.capture());
            assertEquals(USER_ID, mrCaptor.getValue().getFinalUserId());
            assertNotNull(mrCaptor.getValue().getFinishTime());
        }
    }
}
