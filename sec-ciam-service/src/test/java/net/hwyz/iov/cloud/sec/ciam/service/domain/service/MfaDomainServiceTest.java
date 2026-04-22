package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeScene;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMfaChallengeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.MfaChallengePo;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MfaDomainServiceTest {

    private CiamMfaChallengeRepository challengeRepository;
    private SmsAdapter smsAdapter;
    private EmailAdapter emailAdapter;
    private MfaDomainService service;

    @BeforeEach
    void setUp() {
        challengeRepository = mock(CiamMfaChallengeRepository.class);
        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        when(challengeRepository.insert(any())).thenReturn(1);
        when(challengeRepository.updateByChallengeId(any())).thenReturn(1);
        when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                .thenReturn(AdapterResult.ok());
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(AdapterResult.ok());
        service = new MfaDomainService(challengeRepository, smsAdapter, emailAdapter);
    }

    // ---- createChallenge ----

    @Nested
    class CreateChallengeTests {

        @Test
        void createChallenge_sms_insertsRecordAndSendsCode() {
            String challengeId = service.createChallenge(
                    "user-001", "session-001",
                    ChallengeType.SMS, ChallengeScene.NEW_DEVICE,
                    "138****1234", "risk-001");

            assertNotNull(challengeId);
            assertEquals(32, challengeId.length());

            ArgumentCaptor<MfaChallengePo> captor = ArgumentCaptor.forClass(MfaChallengePo.class);
            verify(challengeRepository).insert(captor.capture());
            MfaChallengePo saved = captor.getValue();

            assertEquals("user-001", saved.getUserId());
            assertEquals("session-001", saved.getSessionId());
            assertEquals(ChallengeType.SMS.getCode(), saved.getChallengeType());
            assertEquals(ChallengeScene.NEW_DEVICE.getCode(), saved.getChallengeScene());
            assertEquals("138****1234", saved.getReceiverMask());
            assertEquals(ChallengeStatus.PENDING.getCode(), saved.getChallengeStatus());
            assertEquals("risk-001", saved.getRiskEventId());
            assertNotNull(saved.getVerifyCodeHash());
            assertEquals(64, saved.getVerifyCodeHash().length()); // SHA-256 hex
            assertNotNull(saved.getSendTime());
            assertNotNull(saved.getExpireTime());
            assertTrue(saved.getExpireTime().isAfter(saved.getSendTime()));
            assertEquals(1, saved.getRowVersion());
            assertEquals(1, saved.getRowValid());

            verify(smsAdapter).sendVerificationCode(eq("138****1234"), eq("+86"), anyString());
            verify(emailAdapter, never()).sendVerificationCode(anyString(), anyString());
        }

        @Test
        void createChallenge_email_insertsRecordAndSendsCode() {
            String challengeId = service.createChallenge(
                    "user-002", null,
                    ChallengeType.EMAIL, ChallengeScene.GEO_CHANGE,
                    "t***@example.com", null);

            assertNotNull(challengeId);

            ArgumentCaptor<MfaChallengePo> captor = ArgumentCaptor.forClass(MfaChallengePo.class);
            verify(challengeRepository).insert(captor.capture());
            MfaChallengePo saved = captor.getValue();

            assertEquals(ChallengeType.EMAIL.getCode(), saved.getChallengeType());
            assertEquals(ChallengeScene.GEO_CHANGE.getCode(), saved.getChallengeScene());
            assertNull(saved.getSessionId());
            assertNull(saved.getRiskEventId());

            verify(emailAdapter).sendVerificationCode(eq("t***@example.com"), anyString());
            verify(smsAdapter, never()).sendVerificationCode(anyString(), anyString(), anyString());
        }

        @Test
        void createChallenge_highRiskScene() {
            String challengeId = service.createChallenge(
                    "user-003", "session-003",
                    ChallengeType.SMS, ChallengeScene.HIGH_RISK,
                    "139****5678", "risk-002");

            assertNotNull(challengeId);

            ArgumentCaptor<MfaChallengePo> captor = ArgumentCaptor.forClass(MfaChallengePo.class);
            verify(challengeRepository).insert(captor.capture());
            assertEquals(ChallengeScene.HIGH_RISK.getCode(), captor.getValue().getChallengeScene());
        }

        @Test
        void createChallenge_throwsWhenSmsSendFails() {
            when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                    .thenReturn(AdapterResult.fail("provider error"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createChallenge("user-001", null,
                            ChallengeType.SMS, ChallengeScene.NEW_DEVICE,
                            "138****1234", null));
            assertEquals(CiamErrorCode.MFA_SEND_FAILED, ex.getErrorCode());
            // record should still be inserted before send attempt
            verify(challengeRepository).insert(any());
        }

        @Test
        void createChallenge_throwsWhenEmailSendFails() {
            when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                    .thenReturn(AdapterResult.fail("smtp error"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createChallenge("user-001", null,
                            ChallengeType.EMAIL, ChallengeScene.GEO_CHANGE,
                            "t***@example.com", null));
            assertEquals(CiamErrorCode.MFA_SEND_FAILED, ex.getErrorCode());
        }

        @Test
        void createChallenge_sms_expireTimeIs5MinutesAfterSendTime() {
            service.createChallenge("user-001", null,
                    ChallengeType.SMS, ChallengeScene.NEW_DEVICE,
                    "138****1234", null);

            ArgumentCaptor<MfaChallengePo> captor = ArgumentCaptor.forClass(MfaChallengePo.class);
            verify(challengeRepository).insert(captor.capture());
            MfaChallengePo saved = captor.getValue();

            long diffMinutes = java.time.Duration.between(saved.getSendTime(), saved.getExpireTime()).toMinutes();
            assertEquals(MfaDomainService.SMS_TTL_MINUTES, diffMinutes);
        }

        @Test
        void createChallenge_email_expireTimeIs30MinutesAfterSendTime() {
            service.createChallenge("user-001", null,
                    ChallengeType.EMAIL, ChallengeScene.NEW_DEVICE,
                    "t***@example.com", null);

            ArgumentCaptor<MfaChallengePo> captor = ArgumentCaptor.forClass(MfaChallengePo.class);
            verify(challengeRepository).insert(captor.capture());
            MfaChallengePo saved = captor.getValue();

            long diffMinutes = java.time.Duration.between(saved.getSendTime(), saved.getExpireTime()).toMinutes();
            assertEquals(MfaDomainService.EMAIL_TTL_MINUTES, diffMinutes);
        }
    }

    // ---- verifyChallenge ----

    @Nested
    class VerifyChallengeTests {

        private MfaChallengePo pendingChallenge(String code) {
            MfaChallengePo c = new MfaChallengePo();
            c.setChallengeId("ch-001");
            c.setUserId("user-001");
            c.setChallengeType(ChallengeType.SMS.getCode());
            c.setChallengeScene(ChallengeScene.NEW_DEVICE.getCode());
            c.setVerifyCodeHash(TokenDigest.fingerprint(code));
            c.setSendTime(Instant.now().minusSeconds(1 * 60));
            c.setExpireTime(Instant.now().plusSeconds(4 * 60));
            c.setChallengeStatus(ChallengeStatus.PENDING.getCode());
            return c;
        }

        @Test
        void verifyChallenge_returnsTrueOnCorrectCode() {
            MfaChallengePo challenge = pendingChallenge("123456");
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            boolean result = service.verifyChallenge("ch-001", "123456");

            assertTrue(result);
            assertEquals(ChallengeStatus.PASSED.getCode(), challenge.getChallengeStatus());
            assertNotNull(challenge.getVerifyTime());
            verify(challengeRepository).updateByChallengeId(any());
        }

        @Test
        void verifyChallenge_returnsFalseOnWrongCode() {
            MfaChallengePo challenge = pendingChallenge("123456");
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            boolean result = service.verifyChallenge("ch-001", "999999");

            assertFalse(result);
            assertEquals(ChallengeStatus.FAILED.getCode(), challenge.getChallengeStatus());
            assertNull(challenge.getVerifyTime());
            verify(challengeRepository).updateByChallengeId(any());
        }

        @Test
        void verifyChallenge_throwsWhenNotFound() {
            when(challengeRepository.findByChallengeId("ch-999")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge("ch-999", "123456"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void verifyChallenge_throwsWhenExpired() {
            MfaChallengePo challenge = pendingChallenge("123456");
            challenge.setExpireTime(Instant.now().minusSeconds(1 * 60)); // already expired
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge("ch-001", "123456"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_EXPIRED, ex.getErrorCode());
            assertEquals(ChallengeStatus.EXPIRED.getCode(), challenge.getChallengeStatus());
            verify(challengeRepository).updateByChallengeId(any());
        }

        @Test
        void verifyChallenge_throwsWhenAlreadyPassed() {
            MfaChallengePo challenge = pendingChallenge("123456");
            challenge.setChallengeStatus(ChallengeStatus.PASSED.getCode());
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge("ch-001", "123456"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_ALREADY_RESOLVED, ex.getErrorCode());
        }

        @Test
        void verifyChallenge_throwsWhenAlreadyFailed() {
            MfaChallengePo challenge = pendingChallenge("123456");
            challenge.setChallengeStatus(ChallengeStatus.FAILED.getCode());
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge("ch-001", "123456"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_ALREADY_RESOLVED, ex.getErrorCode());
        }

        @Test
        void verifyChallenge_throwsWhenAlreadyCancelled() {
            MfaChallengePo challenge = pendingChallenge("123456");
            challenge.setChallengeStatus(ChallengeStatus.CANCELLED.getCode());
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge("ch-001", "123456"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_ALREADY_RESOLVED, ex.getErrorCode());
        }
    }

    // ---- cancelChallenge ----

    @Nested
    class CancelChallengeTests {

        @Test
        void cancelChallenge_setsCancelledStatus() {
            MfaChallengePo challenge = new MfaChallengePo();
            challenge.setChallengeId("ch-001");
            challenge.setChallengeStatus(ChallengeStatus.PENDING.getCode());
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            service.cancelChallenge("ch-001");

            assertEquals(ChallengeStatus.CANCELLED.getCode(), challenge.getChallengeStatus());
            assertNotNull(challenge.getModifyTime());
            verify(challengeRepository).updateByChallengeId(any());
        }

        @Test
        void cancelChallenge_throwsWhenNotFound() {
            when(challengeRepository.findByChallengeId("ch-999")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelChallenge("ch-999"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        void cancelChallenge_throwsWhenAlreadyPassed() {
            MfaChallengePo challenge = new MfaChallengePo();
            challenge.setChallengeId("ch-001");
            challenge.setChallengeStatus(ChallengeStatus.PASSED.getCode());
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelChallenge("ch-001"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_ALREADY_RESOLVED, ex.getErrorCode());
        }

        @Test
        void cancelChallenge_throwsWhenAlreadyCancelled() {
            MfaChallengePo challenge = new MfaChallengePo();
            challenge.setChallengeId("ch-001");
            challenge.setChallengeStatus(ChallengeStatus.CANCELLED.getCode());
            when(challengeRepository.findByChallengeId("ch-001")).thenReturn(Optional.of(challenge));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelChallenge("ch-001"));
            assertEquals(CiamErrorCode.MFA_CHALLENGE_ALREADY_RESOLVED, ex.getErrorCode());
        }
    }

    // ---- generateCode ----

    @Nested
    class GenerateCodeTests {

        @Test
        void generateCode_returns6DigitString() {
            String code = service.generateCode();
            assertNotNull(code);
            assertEquals(MfaDomainService.CODE_LENGTH, code.length());
            assertTrue(code.matches("\\d{6}"));
        }

        @Test
        void generateCode_padsWithLeadingZeros() {
            // Run multiple times to increase chance of hitting a small number
            for (int i = 0; i < 100; i++) {
                String code = service.generateCode();
                assertEquals(MfaDomainService.CODE_LENGTH, code.length());
            }
        }
    }
}
