package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.cache.InMemoryVerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VerificationCodeServiceTest {

    private InMemoryVerificationCodeStore codeStore;
    private SmsAdapter smsAdapter;
    private EmailAdapter emailAdapter;
    private VerificationCodeService service;

    private static final String USER_ID = "user-001";
    private static final String CLIENT_ID = "app-001";
    private static final String MOBILE = "13800138000";
    private static final String COUNTRY_CODE = "+86";
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        codeStore = new InMemoryVerificationCodeStore();
        smsAdapter = mock(SmsAdapter.class);
        emailAdapter = mock(EmailAdapter.class);
        when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                .thenReturn(AdapterResult.ok());
        when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                .thenReturn(AdapterResult.ok());
        service = new VerificationCodeService(codeStore, smsAdapter, emailAdapter);
    }

    // ---- 验证码生成 ----

    @Nested
    class CodeGenerationTests {

        @Test
        void generateCode_returns6DigitString() {
            String code = service.generateCode();
            assertNotNull(code);
            assertEquals(6, code.length());
            assertTrue(code.matches("\\d{6}"));
        }

        @Test
        void generateCode_padsWithLeadingZeros() {
            // 多次生成确保格式一致
            for (int i = 0; i < 50; i++) {
                String code = service.generateCode();
                assertEquals(6, code.length());
            }
        }
    }

    // ---- 短信验证码发送 ----

    @Nested
    class SendSmsCodeTests {

        @Test
        void sendSmsCode_storesCodeAndCallsAdapter() {
            service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID);

            verify(smsAdapter).sendVerificationCode(eq(MOBILE), eq(COUNTRY_CODE), anyString());
            // 验证码已存储
            String codeKey = VerificationCodeService.buildCodeKey(USER_ID, CLIENT_ID, VerificationCodeType.SMS);
            assertTrue(codeStore.getCode(codeKey).isPresent());
        }

        @Test
        void sendSmsCode_deletesCodeWhenAdapterFails() {
            when(smsAdapter.sendVerificationCode(anyString(), anyString(), anyString()))
                    .thenReturn(AdapterResult.fail("provider error"));

            assertThrows(BusinessException.class,
                    () -> service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID));

            String codeKey = VerificationCodeService.buildCodeKey(USER_ID, CLIENT_ID, VerificationCodeType.SMS);
            assertTrue(codeStore.getCode(codeKey).isEmpty());
        }
    }

    // ---- 邮箱验证码发送 ----

    @Nested
    class SendEmailCodeTests {

        @Test
        void sendEmailCode_storesCodeAndCallsAdapter() {
            service.sendEmailCode(EMAIL, USER_ID, CLIENT_ID);

            verify(emailAdapter).sendVerificationCode(eq(EMAIL), anyString());
            String codeKey = VerificationCodeService.buildCodeKey(USER_ID, CLIENT_ID, VerificationCodeType.EMAIL);
            assertTrue(codeStore.getCode(codeKey).isPresent());
        }

        @Test
        void sendEmailCode_deletesCodeWhenAdapterFails() {
            when(emailAdapter.sendVerificationCode(anyString(), anyString()))
                    .thenReturn(AdapterResult.fail("smtp error"));

            assertThrows(BusinessException.class,
                    () -> service.sendEmailCode(EMAIL, USER_ID, CLIENT_ID));

            String codeKey = VerificationCodeService.buildCodeKey(USER_ID, CLIENT_ID, VerificationCodeType.EMAIL);
            assertTrue(codeStore.getCode(codeKey).isEmpty());
        }
    }

    // ---- 验证码校验 ----

    @Nested
    class VerifyCodeTests {

        @Test
        void verifyCode_succeedsWithCorrectCode() {
            service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID);

            String codeKey = VerificationCodeService.buildCodeKey(USER_ID, CLIENT_ID, VerificationCodeType.SMS);
            String storedCode = codeStore.getCode(codeKey).orElseThrow();

            assertDoesNotThrow(() ->
                    service.verifyCode(USER_ID, CLIENT_ID, VerificationCodeType.SMS, storedCode));

            // 验证后应被删除
            assertTrue(codeStore.getCode(codeKey).isEmpty());
        }

        @Test
        void verifyCode_throwsOnWrongCode() {
            service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyCode(USER_ID, CLIENT_ID, VerificationCodeType.SMS, "000000"));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());
        }

        @Test
        void verifyCode_throwsWhenNoCodeExists() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyCode(USER_ID, CLIENT_ID, VerificationCodeType.SMS, "123456"));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());
        }

        @Test
        void verifyCode_cannotReuseAfterSuccess() {
            service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID);

            String codeKey = VerificationCodeService.buildCodeKey(USER_ID, CLIENT_ID, VerificationCodeType.SMS);
            String storedCode = codeStore.getCode(codeKey).orElseThrow();

            service.verifyCode(USER_ID, CLIENT_ID, VerificationCodeType.SMS, storedCode);

            // 第二次使用同一验证码应失败
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.verifyCode(USER_ID, CLIENT_ID, VerificationCodeType.SMS, storedCode));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, ex.getErrorCode());
        }
    }

    // ---- 频控 ----

    @Nested
    class RateLimitTests {

        @Test
        void sendSmsCode_rateLimitedWithin1Minute() {
            service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, ex.getErrorCode());
        }

        @Test
        void sendEmailCode_rateLimitedWithin1Minute() {
            service.sendEmailCode(EMAIL, USER_ID, CLIENT_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.sendEmailCode(EMAIL, USER_ID, CLIENT_ID));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, ex.getErrorCode());
        }

        @Test
        void sendSmsCode_differentClientsNotRateLimited() {
            service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, "client-A");

            // 不同客户端不受频控限制
            assertDoesNotThrow(() ->
                    service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, "client-B"));
        }

        @Test
        void sendSmsCode_dailyLimitExceeded() {
            // 模拟达到每日上限
            String dailyKey = VerificationCodeService.buildDailyCountKey(USER_ID, VerificationCodeType.SMS);
            for (int i = 0; i < VerificationCodeService.DAILY_LIMIT; i++) {
                codeStore.incrementDailyCount(dailyKey, 86400);
            }

            // 清除分钟频控以便测试每日限制
            String minuteKey = VerificationCodeService.buildMinuteRateKey(USER_ID, CLIENT_ID, VerificationCodeType.SMS);
            codeStore.deleteCode(minuteKey);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.sendSmsCode(MOBILE, COUNTRY_CODE, USER_ID, CLIENT_ID));
            assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, ex.getErrorCode());
        }
    }

    // ---- Key 构建 ----

    @Nested
    class KeyBuildingTests {

        @Test
        void buildCodeKey_containsTypeUserAndClient() {
            String key = VerificationCodeService.buildCodeKey("u1", "c1", VerificationCodeType.SMS);
            assertEquals("vc:sms:u1:c1", key);
        }

        @Test
        void buildCodeKey_emailType() {
            String key = VerificationCodeService.buildCodeKey("u1", "c1", VerificationCodeType.EMAIL);
            assertEquals("vc:email:u1:c1", key);
        }

        @Test
        void buildMinuteRateKey_format() {
            String key = VerificationCodeService.buildMinuteRateKey("u1", "c1", VerificationCodeType.SMS);
            assertEquals("vc:rate:sms:u1:c1", key);
        }

        @Test
        void buildDailyCountKey_format() {
            String key = VerificationCodeService.buildDailyCountKey("u1", VerificationCodeType.SMS);
            assertEquals("vc:daily:sms:u1", key);
        }
    }

    // ---- TTL 区分 ----

    @Nested
    class TtlTests {

        @Test
        void smsTtl_is5Minutes() {
            assertEquals(300, VerificationCodeType.SMS.getTtlSeconds());
        }

        @Test
        void emailTtl_is30Minutes() {
            assertEquals(1800, VerificationCodeType.EMAIL.getTtlSeconds());
        }
    }
}
