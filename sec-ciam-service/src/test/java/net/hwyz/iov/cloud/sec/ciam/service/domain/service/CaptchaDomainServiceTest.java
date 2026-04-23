package net.hwyz.iov.cloud.sec.ciam.service.domain.service;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.*;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.CaptchaChallenge;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.gatewayimpl.http.StubCaptchaAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.cache.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaptchaDomainServiceTest {

    private InMemoryVerificationCodeStore codeStore;
    private StubCaptchaAdapter captchaAdapter;
    private CaptchaDomainService service;

    private static final String SESSION_ID = "session-001";

    @BeforeEach
    void setUp() {
        codeStore = new InMemoryVerificationCodeStore();
        captchaAdapter = new StubCaptchaAdapter();
        service = new CaptchaDomainService(captchaAdapter, codeStore);
    }

    // ---- 创建挑战 ----

    @Nested
    class CreateChallengeTests {

        @Test
        void createChallenge_returnsChallengeWithIdAndData() {
            CaptchaChallenge challenge = service.createChallenge(SESSION_ID);

            assertNotNull(challenge);
            assertNotNull(challenge.getChallengeId());
            assertFalse(challenge.getChallengeId().isEmpty());
            assertEquals(CaptchaChallenge.CaptchaType.IMAGE, challenge.getChallengeType());
            assertNotNull(challenge.getChallengeData());
            assertTrue(challenge.getChallengeData().contains("="));
        }

        @Test
        void createChallenge_storesChallengeState() {
            CaptchaChallenge challenge = service.createChallenge(SESSION_ID);

            String key = CaptchaDomainService.buildKey(challenge.getChallengeId());
            assertTrue(codeStore.getCode(key).isPresent());
            assertEquals("PENDING", codeStore.getCode(key).get());
        }

        @Test
        void createChallenge_generatesUniqueChallengeIds() {
            CaptchaChallenge c1 = service.createChallenge(SESSION_ID);
            CaptchaChallenge c2 = service.createChallenge(SESSION_ID);

            assertNotEquals(c1.getChallengeId(), c2.getChallengeId());
        }
    }

    // ---- 校验挑战 ----

    @Nested
    class VerifyChallengeTests {

        @Test
        void verifyChallenge_succeedsWithCorrectAnswer() {
            CaptchaChallenge challenge = service.createChallenge(SESSION_ID);
            String answer = extractAnswer(challenge.getChallengeData());

            assertDoesNotThrow(() -> service.verifyChallenge(challenge.getChallengeId(), answer));
        }

        @Test
        void verifyChallenge_removesStateAfterSuccess() {
            CaptchaChallenge challenge = service.createChallenge(SESSION_ID);
            String answer = extractAnswer(challenge.getChallengeData());

            service.verifyChallenge(challenge.getChallengeId(), answer);

            String key = CaptchaDomainService.buildKey(challenge.getChallengeId());
            assertTrue(codeStore.getCode(key).isEmpty());
        }

        @Test
        void verifyChallenge_throwsOnWrongAnswer() {
            CaptchaChallenge challenge = service.createChallenge(SESSION_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge(challenge.getChallengeId(), "99999"));
            assertEquals(CiamErrorCode.CAPTCHA_INVALID, ex.getErrorCode());
        }

        @Test
        void verifyChallenge_removesStateAfterFailure() {
            CaptchaChallenge challenge = service.createChallenge(SESSION_ID);

            assertThrows(BusinessException.class,
                    () -> service.verifyChallenge(challenge.getChallengeId(), "99999"));

            String key = CaptchaDomainService.buildKey(challenge.getChallengeId());
            assertTrue(codeStore.getCode(key).isEmpty());
        }

        @Test
        void verifyChallenge_throwsWhenChallengeNotFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge("nonexistent-id", "42"));
            assertEquals(CiamErrorCode.CAPTCHA_INVALID, ex.getErrorCode());
        }

        @Test
        void verifyChallenge_cannotReuseAfterSuccess() {
            CaptchaChallenge challenge = service.createChallenge(SESSION_ID);
            String answer = extractAnswer(challenge.getChallengeData());

            service.verifyChallenge(challenge.getChallengeId(), answer);

            // 第二次使用同一挑战应失败
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyChallenge(challenge.getChallengeId(), answer));
            assertEquals(CiamErrorCode.CAPTCHA_INVALID, ex.getErrorCode());
        }
    }

    // ---- Key 构建 ----

    @Nested
    class KeyBuildingTests {

        @Test
        void buildKey_containsPrefix() {
            String key = CaptchaDomainService.buildKey("abc-123");
            assertEquals("captcha:abc-123", key);
        }
    }

    // ---- 辅助方法 ----

    /**
     * 从数学表达式（如 "3 + 5 = ?"）中计算答案。
     */
    private String extractAnswer(String challengeData) {
        // 格式: "a + b = ?"
        String expr = challengeData.replace("= ?", "").trim();
        String[] parts = expr.split("\\+");
        int a = Integer.parseInt(parts[0].trim());
        int b = Integer.parseInt(parts[1].trim());
        return String.valueOf(a + b);
    }
}
