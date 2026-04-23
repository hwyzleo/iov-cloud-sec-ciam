package net.hwyz.iov.cloud.sec.ciam.service.domain.service;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.TokenStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamRefreshTokenRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RefreshTokenPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RefreshTokenDomainServiceTest {

    private CiamRefreshTokenRepository refreshTokenRepository;
    private RefreshTokenDomainService service;

    private static final String USER_ID = "user-001";
    private static final String SESSION_ID = "session-001";
    private static final String CLIENT_ID = "client-app";
    private static final int TTL_SECONDS = 2592000; // 30 days

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(CiamRefreshTokenRepository.class);
        when(refreshTokenRepository.insert(any())).thenReturn(1);
        when(refreshTokenRepository.updateByRefreshTokenId(any())).thenReturn(1);
        service = new RefreshTokenDomainService(refreshTokenRepository);
    }

    private RefreshTokenPo stubToken(String rawToken, TokenStatus status, Instant expireTime) {
        RefreshTokenPo token = new RefreshTokenPo();
        token.setRefreshTokenId("rt-" + System.nanoTime());
        token.setUserId(USER_ID);
        token.setSessionId(SESSION_ID);
        token.setClientId(CLIENT_ID);
        token.setTokenFingerprint(TokenDigest.fingerprint(rawToken));
        token.setTokenStatus(status.getCode());
        token.setIssueTime(Instant.now().minusSeconds(1L * 86400));
        token.setExpireTime(expireTime);
        token.setRowValid(1);
        token.setRowVersion(1);
        return token;
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
            ArgumentCaptor<RefreshTokenPo> captor = ArgumentCaptor.forClass(RefreshTokenPo.class);

            String rawToken = service.issueRefreshToken(USER_ID, SESSION_ID, CLIENT_ID, TTL_SECONDS);

            verify(refreshTokenRepository).insert(captor.capture());
            RefreshTokenPo saved = captor.getValue();

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

    // ---- rotateRefreshToken ----

    @Nested
    class RotateTests {

        @Test
        void rotate_returnsNewTokenAndUserInfo() {
            String oldRawToken = "old-raw-token-value";
            RefreshTokenPo existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            RefreshTokenRotationResult result = service.rotateRefreshToken(oldRawToken, CLIENT_ID);

            assertNotNull(result.getNewRefreshToken());
            assertNotEquals(oldRawToken, result.getNewRefreshToken());
            assertEquals(USER_ID, result.getUserId());
            assertEquals(SESSION_ID, result.getSessionId());
        }

        @Test
        void rotate_marksOldTokenAsRotated() {
            String oldRawToken = "old-raw-token-value";
            RefreshTokenPo existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            service.rotateRefreshToken(oldRawToken, CLIENT_ID);

            assertEquals(TokenStatus.ROTATED.getCode(), existing.getTokenStatus());
            assertNotNull(existing.getUsedTime());
            verify(refreshTokenRepository).updateByRefreshTokenId(existing);
        }

        @Test
        void rotate_insertsNewTokenWithParentLink() {
            String oldRawToken = "old-raw-token-value";
            RefreshTokenPo existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().plusSeconds(29L * 86400));
            String oldTokenId = existing.getRefreshTokenId();
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            service.rotateRefreshToken(oldRawToken, CLIENT_ID);

            // insert called twice would be wrong; first call is for the new token
            ArgumentCaptor<RefreshTokenPo> captor = ArgumentCaptor.forClass(RefreshTokenPo.class);
            verify(refreshTokenRepository).insert(captor.capture());
            RefreshTokenPo newToken = captor.getValue();

            assertEquals(oldTokenId, newToken.getParentTokenId());
            assertEquals(TokenStatus.ACTIVE.getCode(), newToken.getTokenStatus());
            assertEquals(USER_ID, newToken.getUserId());
            assertEquals(SESSION_ID, newToken.getSessionId());
            assertEquals(CLIENT_ID, newToken.getClientId());
        }

        @Test
        void rotate_throwsWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenFingerprint(anyString()))
                    .thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.rotateRefreshToken("unknown-token", CLIENT_ID));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        void rotate_throwsWhenTokenExpired() {
            String oldRawToken = "expired-token";
            RefreshTokenPo existing = stubToken(oldRawToken, TokenStatus.ACTIVE,
                    Instant.now().minusSeconds(1 * 3600));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.rotateRefreshToken(oldRawToken, CLIENT_ID));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
            verify(refreshTokenRepository, never()).insert(any());
        }

        @Test
        void rotate_throwsAndRevokesSessionOnReplay() {
            String oldRawToken = "already-rotated-token";
            RefreshTokenPo existing = stubToken(oldRawToken, TokenStatus.ROTATED,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));
            when(refreshTokenRepository.revokeAllBySessionId(SESSION_ID)).thenReturn(3);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.rotateRefreshToken(oldRawToken, CLIENT_ID));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());

            // 重放攻击应撤销该会话所有令牌
            verify(refreshTokenRepository).revokeAllBySessionId(SESSION_ID);
            verify(refreshTokenRepository, never()).insert(any());
        }

        @Test
        void rotate_throwsWhenTokenRevoked() {
            String oldRawToken = "revoked-token";
            RefreshTokenPo existing = stubToken(oldRawToken, TokenStatus.REVOKED,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(oldRawToken)))
                    .thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.rotateRefreshToken(oldRawToken, CLIENT_ID));
            assertEquals(CiamErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }

    // ---- revokeRefreshToken ----

    @Nested
    class RevokeTests {

        @Test
        void revoke_setsTokenToRevokedStatus() {
            String rawToken = "active-token";
            RefreshTokenPo existing = stubToken(rawToken, TokenStatus.ACTIVE,
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
            RefreshTokenPo existing = stubToken(rawToken, TokenStatus.REVOKED,
                    Instant.now().plusSeconds(29L * 86400));
            when(refreshTokenRepository.findByTokenFingerprint(TokenDigest.fingerprint(rawToken)))
                    .thenReturn(Optional.of(existing));

            service.revokeRefreshToken(rawToken);

            verify(refreshTokenRepository, never()).updateByRefreshTokenId(any());
        }
    }
}
