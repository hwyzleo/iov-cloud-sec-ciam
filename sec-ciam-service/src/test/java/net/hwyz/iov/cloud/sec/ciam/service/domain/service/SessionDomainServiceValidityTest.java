package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.DeviceRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会话有效性规则测试（SEC-CIAM-DSN-CR-003 §5.4，TC-003-5/8）。
 */
class SessionDomainServiceValidityTest {

    private SessionRepository sessionRepository;
    private SessionDomainService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        service = new SessionDomainService(sessionRepository,
                mock(RefreshTokenRepository.class), mock(DeviceRepository.class));
    }

    private Session session(int status, Instant expireTime) {
        return Session.builder()
                .sessionId("session-001")
                .userId("user-001")
                .clientId("ciam-mobile-app")
                .clientType("MOBILE")
                .deviceId("device-001")
                .scope("openid profile")
                .sessionStatus(status)
                .expireTime(expireTime)
                .build();
    }

    @Nested
    class OpenSessionTests {

        @Test
        void openSession_freezesContextAndStatusActive() {
            service.openSession("user-001", "ciam-mobile-app", "MOBILE", "device-001",
                    "openid profile", Instant.now().plusSeconds(2592000));

            org.mockito.ArgumentCaptor<Session> captor =
                    org.mockito.ArgumentCaptor.forClass(Session.class);
            verify(sessionRepository).insert(captor.capture());
            Session saved = captor.getValue();

            assertNotNull(saved.getSessionId());
            assertEquals("user-001", saved.getUserId());
            assertEquals("ciam-mobile-app", saved.getClientId());
            assertEquals("MOBILE", saved.getClientType());
            assertEquals("device-001", saved.getDeviceId());
            assertEquals("openid profile", saved.getScope());
            assertEquals(SessionStatus.ACTIVE.getCode(), saved.getSessionStatus());
            assertNotNull(saved.getLoginTime());
            assertNotNull(saved.getLastActiveTime());
            assertNotNull(saved.getExpireTime());
        }
    }

    @Nested
    class RequireActiveSessionTests {

        @Test
        void activeAndUnexpiredSession_isReturned() {
            when(sessionRepository.findBySessionId("session-001"))
                    .thenReturn(Optional.of(session(SessionStatus.ACTIVE.getCode(),
                            Instant.now().plusSeconds(60))));

            assertNotNull(service.requireActiveSession("session-001"));
        }

        @Test
        void missingSessionId_isTokenInvalid() {
            assertEquals(CiamErrorCode.TOKEN_INVALID,
                    assertThrows(BusinessException.class, () -> service.requireActiveSession(null))
                            .getErrorCode());
            assertEquals(CiamErrorCode.TOKEN_INVALID,
                    assertThrows(BusinessException.class, () -> service.requireActiveSession("  "))
                            .getErrorCode());
        }

        @Test
        void unknownSession_isSessionNotFound() {
            when(sessionRepository.findBySessionId("session-001")).thenReturn(Optional.empty());

            assertEquals(CiamErrorCode.SESSION_NOT_FOUND,
                    assertThrows(BusinessException.class, () -> service.requireActiveSession("session-001"))
                            .getErrorCode());
        }

        @Test
        void kickedOrInvalidSession_isTokenInvalid() {
            when(sessionRepository.findBySessionId("session-001"))
                    .thenReturn(Optional.of(session(SessionStatus.KICKED.getCode(),
                            Instant.now().plusSeconds(60))));

            assertEquals(CiamErrorCode.TOKEN_INVALID,
                    assertThrows(BusinessException.class, () -> service.requireActiveSession("session-001"))
                            .getErrorCode());
        }

        @Test
        void expiredSession_isSessionExpired() {
            when(sessionRepository.findBySessionId("session-001"))
                    .thenReturn(Optional.of(session(SessionStatus.ACTIVE.getCode(),
                            Instant.now().minusSeconds(1))));

            assertEquals(CiamErrorCode.SESSION_EXPIRED,
                    assertThrows(BusinessException.class, () -> service.requireActiveSession("session-001"))
                            .getErrorCode());
        }

        @Test
        void nowEqualsExpireTime_isRejected() {
            // now >= expireTime 统一视为过期：用"恰好等于当前秒"的边界验证
            Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
            when(sessionRepository.findBySessionId("session-001"))
                    .thenReturn(Optional.of(session(SessionStatus.ACTIVE.getCode(), now)));

            assertEquals(CiamErrorCode.SESSION_EXPIRED,
                    assertThrows(BusinessException.class, () -> service.requireActiveSession("session-001"))
                            .getErrorCode());
        }
    }

    @Nested
    class ClaimBindingTests {

        private final Session bound = Session.builder()
                .userId("user-001")
                .clientId("ciam-mobile-app")
                .clientType("MOBILE")
                .deviceId("device-001")
                .build();

        @Test
        void matchingClaims_pass() {
            assertDoesNotThrow(() -> service.assertClaimBinding(
                    bound, "user-001", "ciam-mobile-app", "MOBILE", "device-001"));
        }

        @Test
        void mismatchedUser_isRejected() {
            assertEquals(CiamErrorCode.TOKEN_INVALID,
                    assertThrows(BusinessException.class, () -> service.assertClaimBinding(
                            bound, "user-002", "ciam-mobile-app", "MOBILE", "device-001"))
                            .getErrorCode());
        }

        @Test
        void mismatchedDevice_isRejected() {
            assertEquals(CiamErrorCode.TOKEN_INVALID,
                    assertThrows(BusinessException.class, () -> service.assertClaimBinding(
                            bound, "user-001", "ciam-mobile-app", "MOBILE", "device-999"))
                            .getErrorCode());
        }

        @Test
        void mismatchedClientType_isRejected() {
            assertEquals(CiamErrorCode.TOKEN_INVALID,
                    assertThrows(BusinessException.class, () -> service.assertClaimBinding(
                            bound, "user-001", "ciam-mobile-app", "TBOX", "device-001"))
                            .getErrorCode());
        }

        @Test
        void missingClaim_isRejected() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.assertClaimBinding(bound, "user-001", "ciam-mobile-app", null, "device-001"));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }
}
