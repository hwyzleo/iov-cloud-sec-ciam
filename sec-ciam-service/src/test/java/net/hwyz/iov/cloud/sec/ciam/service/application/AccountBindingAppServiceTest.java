package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.MergeRequestDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserIdentityDTO;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ReviewStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMergeRequestRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.IdentityDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
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

            UserIdentityDTO result = service.bindIdentity(
                    USER_ID, IdentityType.MOBILE, PHONE, COUNTRY_CODE, BIND_SOURCE);

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(IdentityType.MOBILE.getCode(), result.getIdentityType());
            assertEquals(IdentityStatus.BOUND.getCode(), result.getIdentityStatus());
        }

        @Test
        void returnsSameIdentityWhenAlreadyBoundToSameUser() {
            // Identity already bound to the same user
            CiamUserIdentityDo existing = stubBoundIdentity(USER_ID, IdentityType.MOBILE, PHONE);
            when(identityRepository.findByTypeAndHash(IdentityType.MOBILE.getCode(), PHONE_HASH))
                    .thenReturn(Optional.of(existing));

            UserIdentityDTO result = service.bindIdentity(
                    USER_ID, IdentityType.MOBILE, PHONE, COUNTRY_CODE, BIND_SOURCE);

            // Should return existing identity without creating a new one
            assertEquals(existing.getIdentityId(), result.getIdentityId());
        }
    }

    // ========== Task 10.3: createMergeRequest ==========

    @Nested
    class CreateMergeRequestTests {

        @Test
        void createsMergeRequestWithPendingStatus() {
            MergeRequestDTO result = service.createMergeRequest(
                    USER_ID, OTHER_USER_ID,
                    IdentityType.MOBILE.getCode(), PHONE_HASH, BIND_SOURCE);

            assertNotNull(result);
            assertEquals(USER_ID, result.getSourceUserId());
            assertEquals(OTHER_USER_ID, result.getTargetUserId());
            assertEquals(ReviewStatus.PENDING.getCode(), result.getReviewStatus());
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
        }
    }
}
