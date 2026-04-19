package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserProfileAppService 单元测试。
 * <p>
 * 仅 mock 底层仓储与审计日志，与项目现有测试风格一致。
 */
class UserProfileAppServiceTest {

    private CiamUserProfileRepository profileRepository;
    private AuditLogger auditLogger;
    private UserProfileAppService service;

    private static final String USER_ID = "user-profile-001";
    private static final String PROFILE_ID = "profile-001";

    @BeforeEach
    void setUp() {
        profileRepository = mock(CiamUserProfileRepository.class);
        auditLogger = mock(AuditLogger.class);

        when(profileRepository.updateByProfileId(any())).thenReturn(1);
        when(profileRepository.insert(any())).thenReturn(1);

        service = new UserProfileAppService(profileRepository, auditLogger);
    }

    private CiamUserProfileDo stubProfile() {
        CiamUserProfileDo profile = new CiamUserProfileDo();
        profile.setProfileId(PROFILE_ID);
        profile.setUserId(USER_ID);
        profile.setNickname("旧昵称");
        profile.setAvatarUrl("https://example.com/old.png");
        profile.setGender(0);
        profile.setRowVersion(1);
        profile.setRowValid(1);
        return profile;
    }

    // ---- getProfile ----

    @Nested
    class GetProfileTests {

        @Test
        void returnsProfileWhenExists() {
            CiamUserProfileDo expected = stubProfile();
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(expected));

            CiamUserProfileDo result = service.getProfile(USER_ID);

            assertEquals(PROFILE_ID, result.getProfileId());
            assertEquals(USER_ID, result.getUserId());
            assertEquals("旧昵称", result.getNickname());
        }

        @Test
        void throwsWhenProfileNotFound() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getProfile(USER_ID));
            assertEquals(CiamErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ---- updateProfile ----

    @Nested
    class UpdateProfileTests {

        @Test
        void updatesNicknameAndAvatar() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));

            service.updateProfile(USER_ID, "新昵称", "https://example.com/new.png",
                    null, null, null, null);

            ArgumentCaptor<CiamUserProfileDo> captor = ArgumentCaptor.forClass(CiamUserProfileDo.class);
            verify(profileRepository).updateByProfileId(captor.capture());
            CiamUserProfileDo updated = captor.getValue();
            assertEquals("新昵称", updated.getNickname());
            assertEquals("https://example.com/new.png", updated.getAvatarUrl());
            // Gender should remain unchanged
            assertEquals(0, updated.getGender());
        }

        @Test
        void updatesGenderBirthdayAndRegion() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));

            LocalDate birthday = LocalDate.of(1990, 1, 15);
            service.updateProfile(USER_ID, null, null, 1, birthday, "110000", "北京市");

            ArgumentCaptor<CiamUserProfileDo> captor = ArgumentCaptor.forClass(CiamUserProfileDo.class);
            verify(profileRepository).updateByProfileId(captor.capture());
            CiamUserProfileDo updated = captor.getValue();
            assertEquals(1, updated.getGender());
            assertEquals(birthday, updated.getBirthday());
            assertEquals("110000", updated.getRegionCode());
            assertEquals("北京市", updated.getRegionName());
            // Nickname should remain unchanged
            assertEquals("旧昵称", updated.getNickname());
        }

        @Test
        void logsAuditEventOnUpdate() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));

            service.updateProfile(USER_ID, "新昵称", null, null, null, null, null);

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            AuditEvent event = auditCaptor.getValue();
            assertEquals("PROFILE", event.getEventType());
            assertEquals("资料更新", event.getEventName());
            assertTrue(event.isSuccess());
            assertEquals(USER_ID, event.getUserId());
        }

        @Test
        void throwsWhenProfileNotFoundOnUpdate() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateProfile(USER_ID, "新昵称", null, null, null, null, null));
            assertEquals(CiamErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
            verify(profileRepository, never()).updateByProfileId(any());
        }
    }

    // ---- updateSensitiveField ----

    @Nested
    class UpdateSensitiveFieldTests {

        @Test
        void updatesRealNameWithValidToken() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));

            service.updateSensitiveField(USER_ID, "realName", "张三", "valid-token-123");

            ArgumentCaptor<CiamUserProfileDo> captor = ArgumentCaptor.forClass(CiamUserProfileDo.class);
            verify(profileRepository).updateByProfileId(captor.capture());
            assertEquals("张三", captor.getValue().getRealName());

            ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditLogger).log(auditCaptor.capture());
            assertEquals("PROFILE", auditCaptor.getValue().getEventType());
            assertEquals("敏感字段更新", auditCaptor.getValue().getEventName());
        }

        @Test
        void throwsWhenTokenIsBlank() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateSensitiveField(USER_ID, "realName", "张三", ""));
            assertEquals(CiamErrorCode.SENSITIVE_FIELD_VERIFICATION_REQUIRED, ex.getErrorCode());
            verify(profileRepository, never()).updateByProfileId(any());
        }

        @Test
        void throwsWhenTokenIsNull() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateSensitiveField(USER_ID, "realName", "张三", null));
            assertEquals(CiamErrorCode.SENSITIVE_FIELD_VERIFICATION_REQUIRED, ex.getErrorCode());
            verify(profileRepository, never()).updateByProfileId(any());
        }

        @Test
        void throwsForNonSensitiveField() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateSensitiveField(USER_ID, "nickname", "新昵称", "valid-token"));
            assertEquals(CiamErrorCode.INVALID_PARAM, ex.getErrorCode());
            verify(profileRepository, never()).updateByProfileId(any());
        }

        @Test
        void throwsWhenProfileNotFoundOnSensitiveUpdate() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.updateSensitiveField(USER_ID, "realName", "张三", "valid-token"));
            assertEquals(CiamErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ---- createProfileIfNotExists ----

    @Nested
    class CreateProfileIfNotExistsTests {

        @Test
        void createsProfileWhenNotExists() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            service.createProfileIfNotExists(USER_ID);

            ArgumentCaptor<CiamUserProfileDo> captor = ArgumentCaptor.forClass(CiamUserProfileDo.class);
            verify(profileRepository).insert(captor.capture());
            CiamUserProfileDo created = captor.getValue();
            assertEquals(USER_ID, created.getUserId());
            assertNotNull(created.getProfileId());
            assertEquals(0, created.getGender());
            assertEquals(1, created.getRowVersion());
            assertEquals(1, created.getRowValid());
            assertNotNull(created.getCreateTime());
            assertNotNull(created.getModifyTime());
        }

        @Test
        void skipsCreationWhenProfileAlreadyExists() {
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(stubProfile()));

            service.createProfileIfNotExists(USER_ID);

            verify(profileRepository, never()).insert(any());
        }
    }
}
