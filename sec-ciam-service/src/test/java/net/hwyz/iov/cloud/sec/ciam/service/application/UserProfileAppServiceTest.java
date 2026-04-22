package net.hwyz.iov.cloud.sec.ciam.service.application;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserProfileDto;
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

            UserProfileDto result = service.getProfile(USER_ID);

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
        }
    }
}
