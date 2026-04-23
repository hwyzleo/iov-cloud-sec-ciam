package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.InvitationRelationDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.InvitationRelationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.InvitationRelation;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.InvitationRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InvitationRelationAppService 单元测试。
 * <p>
 * 仅 mock 底层仓储接口与审计日志，与项目现有测试风格保持一致。
 */
class InvitationRelationAppServiceTest {

    private InvitationRelationRepository invitationRelationRepository;
    private AuditLogger auditLogger;
    private InvitationRelationAppService service;

    private static final String USER_ID = "user-001";
    private static final String INVITER_USER_ID = "user-inviter";
    private static final String INVITATION_CODE = "INV2024";
    private static final String CHANNEL_CODE = "CH_APP";
    private static final String CHANNEL_NAME = "手机App推广";

    @BeforeEach
    void setUp() {
        invitationRelationRepository = mock(InvitationRelationRepository.class);
        auditLogger = mock(AuditLogger.class);
        when(invitationRelationRepository.insert(any())).thenReturn(1);

        service = new InvitationRelationAppService(invitationRelationRepository, auditLogger);
    }

    // ========== recordInvitation ==========

    @Nested
    class RecordInvitationTests {

        @Test
        void recordsInvitationSuccessfully() {
            when(invitationRelationRepository.findByInviteeUserId(USER_ID))
                    .thenReturn(Optional.empty());

            InvitationRelationDto result = service.recordInvitation(
                    USER_ID, INVITER_USER_ID, INVITATION_CODE, CHANNEL_CODE, CHANNEL_NAME);

            assertNotNull(result);
            assertNotNull(result.getRelationId());
            assertEquals(USER_ID, result.getInviteeUserId());
            assertEquals(INVITER_USER_ID, result.getInviterUserId());
            assertEquals(INVITATION_CODE, result.getInviteCode());
            assertEquals(CHANNEL_CODE, result.getInviteChannelCode());
            assertEquals(CHANNEL_NAME, result.getInviteActivityCode());
            assertEquals(1, result.getRelationLockFlag());
            assertNotNull(result.getRegisterTime());

            verify(invitationRelationRepository).insert(any(InvitationRelation.class));

            // Verify audit log
            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(captor.capture());
            assertEquals("INVITATION", captor.getValue().getEventType());
            assertTrue(captor.getValue().isSuccess());
        }

        @Test
        void rejectsDuplicateInvitationRecord() {
            InvitationRelation existing = InvitationRelation.builder()
                    .relationId("existing-relation")
                    .inviteeUserId(USER_ID)
                    .inviterUserId(INVITER_USER_ID)
                    .build();

            when(invitationRelationRepository.findByInviteeUserId(USER_ID))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.recordInvitation(
                            USER_ID, INVITER_USER_ID, INVITATION_CODE, CHANNEL_CODE, CHANNEL_NAME));

            assertEquals(CiamErrorCode.INVITATION_ALREADY_EXISTS, ex.getErrorCode());
            verify(invitationRelationRepository, never()).insert(any());
            verify(auditLogger, never()).log(any());
        }

        @Test
        void skipsWhenAllInvitationFieldsAreNull() {
            InvitationRelationDto result = service.recordInvitation(
                    USER_ID, null, null, null, null);

            assertNull(result);
            verify(invitationRelationRepository, never()).findByInviteeUserId(any());
            verify(invitationRelationRepository, never()).insert(any());
            verify(auditLogger, never()).log(any());
        }

        @Test
        void skipsWhenAllInvitationFieldsAreBlank() {
            InvitationRelationDto result = service.recordInvitation(
                    USER_ID, "", "  ", "", "  ");

            assertNull(result);
            verify(invitationRelationRepository, never()).insert(any());
        }

        @Test
        void recordsWithPartialInvitationInfo() {
            // Only channel code provided, no inviter
            when(invitationRelationRepository.findByInviteeUserId(USER_ID))
                    .thenReturn(Optional.empty());

            InvitationRelationDto result = service.recordInvitation(
                    USER_ID, null, null, CHANNEL_CODE, null);

            assertNotNull(result);
            assertEquals(USER_ID, result.getInviteeUserId());
            assertNull(result.getInviterUserId());
            assertNull(result.getInviteCode());
            assertEquals(CHANNEL_CODE, result.getInviteChannelCode());

            verify(invitationRelationRepository).insert(any(InvitationRelation.class));
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.recordInvitation(null, INVITER_USER_ID, INVITATION_CODE, CHANNEL_CODE, CHANNEL_NAME));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenUserIdIsBlank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.recordInvitation("  ", INVITER_USER_ID, INVITATION_CODE, CHANNEL_CODE, CHANNEL_NAME));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }

    // ========== getInvitationRelation ==========

    @Nested
    class GetInvitationRelationTests {

        @Test
        void returnsInvitationRelationWhenExists() {
            InvitationRelation existing = InvitationRelation.builder()
                    .relationId("rel-001")
                    .inviteeUserId(USER_ID)
                    .inviterUserId(INVITER_USER_ID)
                    .inviteCode(INVITATION_CODE)
                    .inviteChannelCode(CHANNEL_CODE)
                    .build();

            when(invitationRelationRepository.findByInviteeUserId(USER_ID))
                    .thenReturn(Optional.of(existing));

            Optional<InvitationRelationDto> result = service.getInvitationRelation(USER_ID);

            assertTrue(result.isPresent());
            assertEquals("rel-001", result.get().getRelationId());
            assertEquals(INVITER_USER_ID, result.get().getInviterUserId());
        }

        @Test
        void returnsEmptyWhenNoInvitationRelation() {
            when(invitationRelationRepository.findByInviteeUserId(USER_ID))
                    .thenReturn(Optional.empty());

            Optional<InvitationRelationDto> result = service.getInvitationRelation(USER_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        void throwsWhenUserIdIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getInvitationRelation(null));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }

        @Test
        void throwsWhenUserIdIsBlank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getInvitationRelation(""));

            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
        }
    }
}
