package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RegisterSource;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDomainServiceTest {

    private CiamUserRepository userRepository;
    private CiamUserProfileRepository userProfileRepository;
    private UserDomainService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(CiamUserRepository.class);
        userProfileRepository = mock(CiamUserProfileRepository.class);
        when(userRepository.insert(any())).thenReturn(1);
        when(userProfileRepository.insert(any())).thenReturn(1);
        service = new UserDomainService(userRepository, userProfileRepository);
    }

    @Test
    void createUser_generatesGloballyUniqueUserId() {
        CiamUserDo user = service.createUser(RegisterSource.MOBILE, "app", null, null);

        assertNotNull(user.getUserId());
        assertEquals(32, user.getUserId().length());
        assertTrue(user.getUserId().matches("[0-9a-f]{32}"));
    }

    @Test
    void createUser_setsInitialStatusToPending() {
        CiamUserDo user = service.createUser(RegisterSource.EMAIL, null, null, null);

        assertEquals(UserStatus.PENDING.getCode(), user.getUserStatus());
    }

    @Test
    void createUser_setsRegisterSourceAndChannel() {
        CiamUserDo user = service.createUser(RegisterSource.WECHAT, "mini_program", null, null);

        assertEquals("wechat", user.getRegisterSource());
        assertEquals("mini_program", user.getRegisterChannel());
    }

    @Test
    void createUser_defaultsBrandCodeToOpenIOV() {
        CiamUserDo user = service.createUser(RegisterSource.MOBILE, null, null, null);

        assertEquals("OPENIOV", user.getBrandCode());
    }

    @Test
    void createUser_usesProvidedBrandCode() {
        CiamUserDo user = service.createUser(RegisterSource.MOBILE, null, "CUSTOM_BRAND", null);

        assertEquals("CUSTOM_BRAND", user.getBrandCode());
    }

    @Test
    void createUser_defaultsBrandCodeWhenBlank() {
        CiamUserDo user = service.createUser(RegisterSource.MOBILE, null, "  ", null);

        assertEquals("OPENIOV", user.getBrandCode());
    }

    @Test
    void createUser_insertsUserRecord() {
        service.createUser(RegisterSource.MOBILE, null, null, null);

        verify(userRepository, times(1)).insert(any(CiamUserDo.class));
    }

    @Test
    void createUser_insertsProfileRecord() {
        service.createUser(RegisterSource.MOBILE, null, null, null);

        ArgumentCaptor<CiamUserProfileDo> captor =
                ArgumentCaptor.forClass(CiamUserProfileDo.class);
        verify(userProfileRepository, times(1)).insert(captor.capture());

        CiamUserProfileDo profile = captor.getValue();
        assertNotNull(profile.getProfileId());
        assertEquals(32, profile.getProfileId().length());
        assertEquals(0, profile.getGender());
        assertEquals(1, profile.getRowValid());
    }

    @Test
    void createUser_profileUserIdMatchesUserUserId() {
        CiamUserDo user = service.createUser(RegisterSource.MOBILE, null, null, null);

        ArgumentCaptor<CiamUserProfileDo> captor =
                ArgumentCaptor.forClass(CiamUserProfileDo.class);
        verify(userProfileRepository).insert(captor.capture());

        assertEquals(user.getUserId(), captor.getValue().getUserId());
    }

    @Test
    void createUser_setsRowVersionAndRowValid() {
        CiamUserDo user = service.createUser(RegisterSource.MOBILE, null, null, null);

        assertEquals(1, user.getRowVersion());
        assertEquals(1, user.getRowValid());
    }

    @Test
    void createUser_setsTimestamps() {
        CiamUserDo user = service.createUser(RegisterSource.MOBILE, null, null, null);

        assertNotNull(user.getCreateTime());
        assertNotNull(user.getModifyTime());
    }

    @Test
    void createUser_twoCalls_produceDifferentUserIds() {
        CiamUserDo user1 = service.createUser(RegisterSource.MOBILE, null, null, null);
        CiamUserDo user2 = service.createUser(RegisterSource.MOBILE, null, null, null);

        assertNotEquals(user1.getUserId(), user2.getUserId());
    }

    // ---- 状态流转测试 ----

    @Nested
    class StatusTransitionTests {

        private CiamUserDo stubUser(UserStatus status) {
            CiamUserDo user = new CiamUserDo();
            user.setUserId("test-user-001");
            user.setUserStatus(status.getCode());
            return user;
        }

        @Test
        void activate_fromPending_setsActive() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.PENDING)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.activate("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.ACTIVE.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void lock_fromActive_setsLocked() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.lock("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.LOCKED.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void unlock_fromLocked_setsActive() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.LOCKED)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.unlock("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.ACTIVE.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void disable_fromActive_setsDisabled() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.disable("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.DISABLED.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void enable_fromDisabled_setsActive() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.DISABLED)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.enable("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.ACTIVE.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void startDeactivation_fromActive_setsDeactivating() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.ACTIVE)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.startDeactivation("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.DEACTIVATING.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void completeDeactivation_fromDeactivating_setsDeactivated() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.DEACTIVATING)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.completeDeactivation("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.DEACTIVATED.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void cancelDeactivation_fromDeactivating_setsActive() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.DEACTIVATING)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.cancelDeactivation("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertEquals(UserStatus.ACTIVE.getCode(), captor.getValue().getUserStatus());
        }

        @Test
        void lock_fromPending_throwsBusinessException() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.PENDING)));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.lock("u1"));
            assertEquals(CiamErrorCode.ILLEGAL_STATUS_TRANSITION, ex.getErrorCode());
            verify(userRepository, never()).updateByUserId(any());
        }

        @Test
        void activate_fromDeactivated_throwsBusinessException() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.DEACTIVATED)));

            BusinessException ex = assertThrows(BusinessException.class, () -> service.activate("u1"));
            assertEquals(CiamErrorCode.ILLEGAL_STATUS_TRANSITION, ex.getErrorCode());
        }

        @Test
        void transition_userNotFound_throwsBusinessException() {
            when(userRepository.findByUserId("missing")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class, () -> service.activate("missing"));
            assertEquals(CiamErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void transition_updatesModifyTime() {
            when(userRepository.findByUserId("u1")).thenReturn(Optional.of(stubUser(UserStatus.PENDING)));
            when(userRepository.updateByUserId(any())).thenReturn(1);

            service.activate("u1");

            ArgumentCaptor<CiamUserDo> captor = ArgumentCaptor.forClass(CiamUserDo.class);
            verify(userRepository).updateByUserId(captor.capture());
            assertNotNull(captor.getValue().getModifyTime());
        }
    }
}
