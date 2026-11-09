package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TokenStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.RefreshToken;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Session;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.SessionStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefreshTokenDomainServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenDomainService service;

    private static final String USER_ID = "user-001";
    private static final String SESSION_ID = "session-001";
    private static final String CLIENT_ID = "client-app";
    private static final int TTL_SECONDS = 2592000; // 30 days

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        when(refreshTokenRepository.insert(any())).thenReturn(1);
        when(refreshTokenRepository.updateByRefreshTokenId(any())).thenReturn(1);
        service = new RefreshTokenDomainService(refreshTokenRepository);
    }

    private RefreshToken stubToken(String rawToken, TokenStatus status, Instant expireTime) {
        return RefreshToken.builder()
                .refreshTokenId("rt-" + System.nanoTime())
                .userId(USER_ID)
                .sessionId(SESSION_ID)
                .clientId(CLIENT_ID)
                .tokenFingerprint(TokenDigest.fingerprint(rawToken))
                .tokenStatus(status.getCode())
                .issueTime(Instant.now().minusSeconds(1L * 86400))
                .expireTime(expireTime)
                .build();
    }

    // ---- issueRefreshToken ----

    @Nested
    class IssueTests {

        @Test
        void issue_returnsNonNullRawToken() {
            String rawToken = service.issueRefreshToken(USER_ID, SESSION_ID, CLIENT_ID, TTL_SECONDS);

            assertNotNull(rawToken);
            assertFalse(rawToken.isBlank());
        }

        @Test
        void issue_persistsTokenWithCorrectFields() {
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            String rawToken = service.issueRefreshToken(USER_ID, SESSION_ID, CLIENT_ID, TTL_SECONDS);

            verify(refreshTokenRepository).insert(captor.capture());
            RefreshToken saved = captor.getValue();

            assertEquals(USER_ID, saved.getUserId());
            assertEquals(SESSION_ID, saved.getSessionId());
            assertEquals(CLIENT_ID, saved.getClientId());
            assertEquals(TokenStatus.ACTIVE.getCode(), saved.getTokenStatus());
            assertEquals(TokenDigest.fingerprint(rawToken), saved.getTokenFingerprint());
            assertNotNull(saved.getRefreshTokenId());
            assertNotNull(saved.getIssueTime());
            assertNotNull(saved.getExpireTime());
            assertNull(saved.getParentTokenId());
        }

        @Test
        void issue_generatesUniqueTokensEachCall() {
            String token1 = service.issueRefreshToken(USER_ID, SESSION_ID, CLIENT_ID, TTL_SECONDS);
            String token2 = service.issueRefreshToken(USER_ID, SESSION_ID, CLIENT_ID, TTL_SECONDS);

            assertNotEquals(token1, token2);
        }
    }

    // ---- loadForRotation + rotate（SEC-CIAM-DSN-CR-003 §5.2）----

    @Nested
    class RotateTests {

        private Session activeSession() {
            return Session.builder()
                    .sessionId(SESSION_ID)
                    .userId(USER_ID)
                    .clientId(CLIENT_ID)
                    .clientType("MOBILE")
                    .deviceId("device-001")
                    .scope("openid profile")
                    .sessionStatus(SessionStatus.ACTIVE.getCode())
                    .expireTime(Instant.now().plusSeconds(29L * 86400))
                    .build();
        }

        private RefreshToken load(String rawToken) {
            return service.loadForRotation(rawToken, CLIENT_ID);
        }

        @Test
        void rotate_returnsContextRestoredFromSession() {
            String oldRawToken = "old-raw-token-value";
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));
            when(refreshTokenRepository.compareAndSetActiveToRotated(anyString(), any())).thenReturn(1);

            RefreshTokenRotationResult result = service.rotate(load(oldRawToken), activeSession());

            assertNotNull(result.getNewRefreshToken());
            assertNotEquals(oldRawToken, result.getNewRefreshToken());
            assertEquals(USER_ID, result.getUserId());
            assertEquals(SESSION_ID, result.getSessionId());
            assertEquals(CLIENT_ID, result.getClientId());
            assertEquals("MOBILE", result.getClientType());
            assertEquals("device-001", result.getDeviceId());
            assertEquals("openid profile", result.getScope());
            assertEquals(existing.getExpireTime(), result.getExpireTime());
        }

        @Test
        void rotate_usesConditionalUpdateAsSingleWinner() {
            String oldRawToken = "old-raw-token-value";
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));
            when(refreshTokenRepository.compareAndSetActiveToRotated(anyString(), any())).thenReturn(1);

            service.rotate(load(oldRawToken), activeSession());

            verify(refreshTokenRepository)
                    .compareAndSetActiveToRotated(eq(existing.getRefreshTokenId()), any());
            // 不再使用"先查状态再普通更新"的非原子写法
            verify(refreshTokenRepository, never()).updateByRefreshTokenId(any());
        }

        @Test
        void rotate_childInheritsAbsoluteExpiry() {
            String oldRawToken = "old-raw-token-value";
            Instant absoluteExpiry = Instant.now().plusSeconds(3L * 86400);
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.ACTIVE, absoluteExpiry);
            String oldTokenId = existing.getRefreshTokenId();
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));
            when(refreshTokenRepository.compareAndSetActiveToRotated(anyString(), any())).thenReturn(1);

            service.rotate(load(oldRawToken), activeSession());

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).insert(captor.capture());
            RefreshToken child = captor.getValue();

            assertEquals(oldTokenId, child.getParentTokenId());
            assertEquals(TokenStatus.ACTIVE.getCode(), child.getTokenStatus());
            assertEquals(USER_ID, child.getUserId());
            assertEquals(SESSION_ID, child.getSessionId());
            assertEquals(CLIENT_ID, child.getClientId());
            // 绝对到期边界不延长
            assertEquals(absoluteExpiry, child.getExpireTime());
        }

        @Test
        void rotate_casFailureRevokesChainAndThrowsReplayException() {
            String oldRawToken = "racing-token";
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));
            when(refreshTokenRepository.compareAndSetActiveToRotated(anyString(), any())).thenReturn(0);

            RefreshToken loaded = load(oldRawToken);
            assertThrows(RefreshTokenReplayException.class,
                    () -> service.rotate(loaded, activeSession()));

            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
            verify(refreshTokenRepository, never()).insert(any());
        }

        @Test
        void load_throwsWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenFingerprint(anyString()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> load("unknown-token"));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        void load_throwsWhenTokenExpired() {
            String oldRawToken = "expired-token";
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().minusSeconds(1 * 3600));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> load(oldRawToken));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
            verify(refreshTokenRepository, never()).insert(any());
        }

        @Test
        void load_throwsWhenClientMismatch() {
            String oldRawToken = "client-mismatch-token";
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.loadForRotation(oldRawToken, "other-client"));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        void load_throwsAndRevokesSessionOnReplay() {
            String oldRawToken = "already-rotated-token";
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.ROTATED,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));
            when(refreshTokenRepository.revokeAllBySessionId(SESSION_ID)).thenReturn(3);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> load(oldRawToken));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
            assertTrue(ex instanceof RefreshTokenReplayException,
                    "重放必须使用专用异常以提交撤销结果");

            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
            verify(refreshTokenRepository, never()).insert(any());
        }

        @Test
        void load_throwsWhenTokenRevoked() {
            String oldRawToken = "revoked-token";
            RefreshToken existing = stubToken(oldRawToken, TokenStatus.REVOKED,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> load(oldRawToken));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }

    // ---- revokeRefreshToken ----

    @Nested
    class RevokeTests {

        @Test
        void revoke_setsTokenToRevokedStatus() {
            String rawToken = "active-token";
            RefreshToken existing = stubToken(rawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            service.revokeRefreshToken(rawToken);

            assertEquals(TokenStatus.REVOKED.getCode(), existing.getTokenStatus());
            assertNotNull(existing.getRevokeTime());
            verify(refreshTokenRepository).updateByRefreshTokenId(existing);
        }

        @Test
        void revoke_throwsWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenFingerprint(anyString()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.revokeRefreshToken("unknown-token"));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        void revoke_skipsWhenAlreadyRevoked() {
            String rawToken = "already-revoked";
            RefreshToken existing = stubToken(rawToken, TokenStatus.REVOKED,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            service.revokeRefreshToken(rawToken);

            verify(refreshTokenRepository, never()).updateByRefreshTokenId(any());
        }
    }
}
