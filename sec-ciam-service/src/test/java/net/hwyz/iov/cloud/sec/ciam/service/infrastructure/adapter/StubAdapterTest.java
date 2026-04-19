package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.adapter;

import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.CaptchaChallenge;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.ThirdPartyUserInfo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StubAdapterTest {

    // ---- SmsAdapter ----

    @Nested
    class LoggingSmsAdapterTests {

        private final LoggingSmsAdapter adapter = new LoggingSmsAdapter();

        @Test
        void sendVerificationCode_returnsSuccess() {
            AdapterResult result = adapter.sendVerificationCode("13800000001", "+86", "123456");
            assertTrue(result.isSuccess());
            assertNotNull(result.getMessage());
        }
    }

    // ---- EmailAdapter ----

    @Nested
    class LoggingEmailAdapterTests {

        private final LoggingEmailAdapter adapter = new LoggingEmailAdapter();

        @Test
        void sendVerificationCode_returnsSuccess() {
            AdapterResult result = adapter.sendVerificationCode("user@example.com", "654321");
            assertTrue(result.isSuccess());
            assertNotNull(result.getMessage());
        }
    }

    // ---- WechatLoginAdapter ----

    @Nested
    class StubWechatLoginAdapterTests {

        private final StubWechatLoginAdapter adapter = new StubWechatLoginAdapter();

        @Test
        void getUserInfo_returnsStubUserInfo() {
            ThirdPartyUserInfo info = adapter.getUserInfo("wx-auth-code");
            assertNotNull(info.getSubject());
            assertNotNull(info.getUnionId());
            assertNotNull(info.getNickname());
            assertNotNull(info.getAvatarUrl());
        }
    }

    // ---- AppleLoginAdapter ----

    @Nested
    class StubAppleLoginAdapterTests {

        private final StubAppleLoginAdapter adapter = new StubAppleLoginAdapter();

        @Test
        void verifyIdentityToken_returnsStubUserInfo() {
            ThirdPartyUserInfo info = adapter.verifyIdentityToken("apple-id-token");
            assertNotNull(info.getSubject());
            assertTrue(info.getSubject().startsWith("apple-stub-subject-"));
            assertNotNull(info.getEmail());
        }
    }

    // ---- GoogleLoginAdapter ----

    @Nested
    class StubGoogleLoginAdapterTests {

        private final StubGoogleLoginAdapter adapter = new StubGoogleLoginAdapter();

        @Test
        void verifyIdToken_returnsStubUserInfo() {
            ThirdPartyUserInfo info = adapter.verifyIdToken("google-id-token");
            assertNotNull(info.getSubject());
            assertTrue(info.getSubject().startsWith("google-stub-subject-"));
            assertNotNull(info.getEmail());
        }
    }

    // ---- LocalMobileAuthAdapter ----

    @Nested
    class StubLocalMobileAuthAdapterTests {

        private final StubLocalMobileAuthAdapter adapter = new StubLocalMobileAuthAdapter();

        @Test
        void verifyToken_returnsPhoneNumber() {
            String phone = adapter.verifyToken("carrier-token");
            assertNotNull(phone);
            assertFalse(phone.isEmpty());
        }
    }

    // ---- CaptchaAdapter ----

    @Nested
    class StubCaptchaAdapterTests {

        private final StubCaptchaAdapter adapter = new StubCaptchaAdapter();

        @Test
        void generateChallenge_returnsChallengeWithIdAndData() {
            var challenge = adapter.generateChallenge("session-1");
            assertNotNull(challenge.getChallengeId());
            assertFalse(challenge.getChallengeId().isEmpty());
            assertNotNull(challenge.getChallengeData());
            assertTrue(challenge.getChallengeData().contains("= ?"));
            assertEquals(CaptchaChallenge.CaptchaType.IMAGE,
                    challenge.getChallengeType());
        }

        @Test
        void verifyChallenge_succeedsWithCorrectAnswer() {
            var challenge = adapter.generateChallenge("session-1");
            String answer = extractMathAnswer(challenge.getChallengeData());
            assertTrue(adapter.verifyChallenge(challenge.getChallengeId(), answer));
        }

        @Test
        void verifyChallenge_failsWithWrongAnswer() {
            var challenge = adapter.generateChallenge("session-1");
            assertFalse(adapter.verifyChallenge(challenge.getChallengeId(), "99999"));
        }

        @Test
        void verifyChallenge_failsWhenChallengeNotFound() {
            assertFalse(adapter.verifyChallenge("nonexistent", "42"));
        }

        @Test
        void verifyChallenge_cannotReuseChallenge() {
            var challenge = adapter.generateChallenge("session-1");
            String answer = extractMathAnswer(challenge.getChallengeData());
            assertTrue(adapter.verifyChallenge(challenge.getChallengeId(), answer));
            // 第二次使用同一挑战应失败
            assertFalse(adapter.verifyChallenge(challenge.getChallengeId(), answer));
        }

        private String extractMathAnswer(String challengeData) {
            String expr = challengeData.replace("= ?", "").trim();
            String[] parts = expr.split("\\+");
            int a = Integer.parseInt(parts[0].trim());
            int b = Integer.parseInt(parts[1].trim());
            return String.valueOf(a + b);
        }
    }

    // ---- AdapterResult ----

    @Nested
    class AdapterResultTests {

        @Test
        void ok_createsSuccessResult() {
            AdapterResult result = AdapterResult.ok();
            assertTrue(result.isSuccess());
            assertEquals("ok", result.getMessage());
        }

        @Test
        void okWithMessage_createsSuccessResultWithMessage() {
            AdapterResult result = AdapterResult.ok("sent");
            assertTrue(result.isSuccess());
            assertEquals("sent", result.getMessage());
        }

        @Test
        void fail_createsFailureResult() {
            AdapterResult result = AdapterResult.fail("timeout");
            assertFalse(result.isSuccess());
            assertEquals("timeout", result.getMessage());
        }
    }
}
