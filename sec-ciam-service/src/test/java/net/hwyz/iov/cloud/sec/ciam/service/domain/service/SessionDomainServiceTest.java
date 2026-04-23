package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.DeviceStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.DeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionDomainServiceTest {

    private SessionRepository sessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private DeviceRepository deviceRepository;
    private SessionDomainService service;

    private static final String SESSION_ID = "session-001";
    private static final String USER_ID = "user-001";
    private static final String DEVICE_ID = "device-001";

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        deviceRepository = mock(DeviceRepository.class);
        when(sessionRepository.updateBySessionId(any())).thenReturn(1);
        when(refreshTokenRepository.revokeAllBySessionId(anyString())).thenReturn(0);
        service = new SessionDomainService(sessionRepository, refreshTokenRepository, deviceRepository);
    }

    private Session stubSession(String sessionId, String userId, SessionStatus status) {
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setSessionStatus(status.getCode());
        return session;
    }

    private Device stubDevice(String deviceId, String userId, DeviceStatus status) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setUserId(userId);
        device.setDeviceStatus(status.getCode());
        return device;
    }

    // ---- logout ----

    @Nested
    class LogoutTests {

        @Test
        void logout_setsSessionToKickedAndRevokesTokens() {
            Session session = stubSession(SESSION_ID, USER_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
            when(refreshTokenRepository.revokeAllBySessionId(SESSION_ID)).thenReturn(2);

            service.logout(SESSION_ID, USER_ID);

            assertEquals(SessionStatus.KICKED.getCode(), session.getSessionStatus());
            assertNotNull(session.getLogoutTime());
            assertNotNull(session.getModifyTime());
            verify(sessionRepository).updateBySessionId(session);
            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
        }

        @Test
        void logout_throwsWhenSessionNotFound() {
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.logout(SESSION_ID, USER_ID));
            assertEquals(CiamErrorCode.SESSION_NOT_FOUND, ex.getErrorCode());
            verify(sessionRepository, never()).updateBySessionId(any());
            verify(refreshTokenRepository, never()).revokeAllBySessionId(anyString());
        }

        @Test
        void logout_throwsWhenUserIdMismatch() {
            Session session = stubSession(SESSION_ID, "other-user", SessionStatus.ACTIVE);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.logout(SESSION_ID, USER_ID));
            assertEquals(CiamErrorCode.FORBIDDEN, ex.getErrorCode());
            verify(sessionRepository, never()).updateBySessionId(any());
            verify(refreshTokenRepository, never()).revokeAllBySessionId(anyString());
        }

        @Test
        void logout_skipsWhenSessionAlreadyOffline() {
            Session session = stubSession(SESSION_ID, USER_ID, SessionStatus.KICKED);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

            service.logout(SESSION_ID, USER_ID);

            verify(sessionRepository, never()).updateBySessionId(any());
            verify(refreshTokenRepository, never()).revokeAllBySessionId(anyString());
        }

        @Test
        void logout_skipsWhenSessionExpired() {
            Session session = stubSession(SESSION_ID, USER_ID, SessionStatus.EXPIRED);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

            service.logout(SESSION_ID, USER_ID);

            verify(sessionRepository, never()).updateBySessionId(any());
            verify(refreshTokenRepository, never()).revokeAllBySessionId(anyString());
        }

        @Test
        void logout_revokesMultipleActiveTokens() {
            Session session = stubSession(SESSION_ID, USER_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
            when(refreshTokenRepository.revokeAllBySessionId(SESSION_ID)).thenReturn(5);

            service.logout(SESSION_ID, USER_ID);

            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
        }
    }

    // ---- invalidateSession ----

    @Nested
    class InvalidateSessionTests {

        @Test
        void invalidateSession_setsSessionToInvalidAndRevokesTokens() {
            Session session = stubSession(SESSION_ID, USER_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
            when(refreshTokenRepository.revokeAllBySessionId(SESSION_ID)).thenReturn(1);

            service.invalidateSession(SESSION_ID);

            assertEquals(SessionStatus.INVALID.getCode(), session.getSessionStatus());
            assertNotNull(session.getLogoutTime());
            assertNotNull(session.getModifyTime());
            verify(sessionRepository).updateBySessionId(session);
            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
        }

        @Test
        void invalidateSession_throwsWhenSessionNotFound() {
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.invalidateSession(SESSION_ID));
            assertEquals(CiamErrorCode.SESSION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void invalidateSession_skipsWhenSessionAlreadyInvalid() {
            Session session = stubSession(SESSION_ID, USER_ID, SessionStatus.INVALID);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

            service.invalidateSession(SESSION_ID);

            verify(sessionRepository, never()).updateBySessionId(any());
            verify(refreshTokenRepository, never()).revokeAllBySessionId(anyString());
        }
    }

    // ---- findUserSessions ----

    @Nested
    class FindUserSessionsTests {

        @Test
        void findUserSessions_returnsActiveSessionsForUser() {
            Session s1 = stubSession("s1", USER_ID, SessionStatus.ACTIVE);
            Session s2 = stubSession("s2", USER_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.ACTIVE.getCode()))
                    .thenReturn(List.of(s1, s2));

            List<Session> result = service.findUserSessions(USER_ID);

            assertEquals(2, result.size());
            verify(sessionRepository).findByUserIdAndStatus(USER_ID, SessionStatus.ACTIVE.getCode());
        }

        @Test
        void findUserSessions_returnsEmptyListWhenNoActiveSessions() {
            when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.ACTIVE.getCode()))
                    .thenReturn(Collections.emptyList());

            List<Session> result = service.findUserSessions(USER_ID);

            assertTrue(result.isEmpty());
        }
    }

    // ---- findUserDevices ----

    @Nested
    class FindUserDevicesTests {

        @Test
        void findUserDevices_returnsActiveDevicesForUser() {
            Device d1 = stubDevice("d1", USER_ID, DeviceStatus.ACTIVE);
            Device d2 = stubDevice("d2", USER_ID, DeviceStatus.ACTIVE);
            when(deviceRepository.findByUserIdAndStatus(USER_ID, DeviceStatus.ACTIVE.getCode()))
                    .thenReturn(List.of(d1, d2));

            List<Device> result = service.findUserDevices(USER_ID);

            assertEquals(2, result.size());
            verify(deviceRepository).findByUserIdAndStatus(USER_ID, DeviceStatus.ACTIVE.getCode());
        }

        @Test
        void findUserDevices_returnsEmptyListWhenNoActiveDevices() {
            when(deviceRepository.findByUserIdAndStatus(USER_ID, DeviceStatus.ACTIVE.getCode()))
                    .thenReturn(Collections.emptyList());

            List<Device> result = service.findUserDevices(USER_ID);

            assertTrue(result.isEmpty());
        }
    }

    // ---- kickSession ----

    @Nested
    class KickSessionTests {

        @Test
        void kickSession_delegatesToLogout() {
            Session session = stubSession(SESSION_ID, USER_ID, SessionStatus.ACTIVE);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
            when(refreshTokenRepository.revokeAllBySessionId(SESSION_ID)).thenReturn(1);

            service.kickSession(SESSION_ID, USER_ID);

            assertEquals(SessionStatus.KICKED.getCode(), session.getSessionStatus());
            assertNotNull(session.getLogoutTime());
            verify(sessionRepository).updateBySessionId(session);
            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
        }

        @Test
        void kickSession_throwsWhenSessionNotFound() {
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.kickSession(SESSION_ID, USER_ID));
            assertEquals(CiamErrorCode.SESSION_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void kickSession_throwsWhenUserIdMismatch() {
            Session session = stubSession(SESSION_ID, "other-user", SessionStatus.ACTIVE);
            when(sessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.kickSession(SESSION_ID, USER_ID));
            assertEquals(CiamErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    // ---- kickDevice ----

    @Nested
    class KickDeviceTests {

        @Test
        void kickDevice_offlineAllSessionsAndInvalidatesDevice() {
            Device device = stubDevice(DEVICE_ID, USER_ID, DeviceStatus.ACTIVE);
            when(deviceRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(device));

            Session s1 = stubSession("s1", USER_ID, SessionStatus.ACTIVE);
            s1.setDeviceId(DEVICE_ID);
            Session s2 = stubSession("s2", USER_ID, SessionStatus.ACTIVE);
            s2.setDeviceId(DEVICE_ID);
            when(sessionRepository.findByDeviceIdAndStatus(DEVICE_ID, SessionStatus.ACTIVE.getCode()))
                    .thenReturn(List.of(s1, s2));

            service.kickDevice(DEVICE_ID, USER_ID);

            // sessions should be kicked
            assertEquals(SessionStatus.KICKED.getCode(), s1.getSessionStatus());
            assertEquals(SessionStatus.KICKED.getCode(), s2.getSessionStatus());
            assertNotNull(s1.getLogoutTime());
            assertNotNull(s2.getLogoutTime());
            verify(sessionRepository).updateBySessionId(s1);
            verify(sessionRepository).updateBySessionId(s2);
            verify(refreshTokenRepository).revokeAllBySessionId("s1");
            verify(refreshTokenRepository).revokeAllBySessionId("s2");

            // device should be invalidated
            assertEquals(DeviceStatus.INVALID.getCode(), device.getDeviceStatus());
            verify(deviceRepository).updateByDeviceId(device);
        }

        @Test
        void kickDevice_throwsWhenDeviceNotFound() {
            when(deviceRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.kickDevice(DEVICE_ID, USER_ID));
            assertEquals(CiamErrorCode.DEVICE_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void kickDevice_throwsWhenUserIdMismatch() {
            Device device = stubDevice(DEVICE_ID, "other-user", DeviceStatus.ACTIVE);
            when(deviceRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(device));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.kickDevice(DEVICE_ID, USER_ID));
            assertEquals(CiamErrorCode.FORBIDDEN, ex.getErrorCode());
        }

        @Test
        void kickDevice_handlesNoActiveSessionsGracefully() {
            Device device = stubDevice(DEVICE_ID, USER_ID, DeviceStatus.ACTIVE);
            when(deviceRepository.findByDeviceId(DEVICE_ID)).thenReturn(Optional.of(device));
            when(sessionRepository.findByDeviceIdAndStatus(DEVICE_ID, SessionStatus.ACTIVE.getCode()))
                    .thenReturn(Collections.emptyList());

            service.kickDevice(DEVICE_ID, USER_ID);

            // device should still be invalidated even with no active sessions
            assertEquals(DeviceStatus.INVALID.getCode(), device.getDeviceStatus());
            verify(deviceRepository).updateByDeviceId(device);
            verify(sessionRepository, never()).updateBySessionId(any());
        }
    }
}
