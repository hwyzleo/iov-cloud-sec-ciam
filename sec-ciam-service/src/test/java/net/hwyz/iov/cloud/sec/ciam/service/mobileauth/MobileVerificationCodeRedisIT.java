package net.hwyz.iov.cloud.sec.ciam.service.mobileauth;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeStore;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证码真实 Redis 集成测试（SEC-CIAM-DSN-CR-003 TC-003-3，US-003-2）。
 * <p>
 * 使用真实 Redis（隔离 DB）与真实 RedisVerificationCodeStore，不 Mock 存储。
 */
@SpringBootTest(classes = CiamMobileAuthItTestApplication.class,
        properties = {
                "ciam.authentication.mobile.allowed-client-ids=ciam-mobile-app",
                "ciam.authentication.verification-code.fixed-enabled=true"
        })
class MobileVerificationCodeRedisIT extends CiamMobileAuthItBase {

    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private VerificationCodeStore codeStore;

    private String uniqueMobile() {
        return "137" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000));
    }

    @Test
    @DisplayName("TC-003-3 固定验证码在 dev/test 生效，单次消费，重复/错误/跨设备拒绝")
    void fixedCode_isSingleUseAndDeviceBound() {
        String mobile = uniqueMobile();
        String deviceId = "DEV-" + System.nanoTime();
        String userKey = FieldEncryptor.hash(mobile);

        verificationCodeService.sendSmsCode(mobile, "+86", userKey, deviceId);

        String codeKey = VerificationCodeService.buildCodeKey(userKey, deviceId, VerificationCodeType.SMS);
        assertEquals("111111", codeStore.getCode(codeKey).orElse(null), "dev/test 固定验证码应为 111111");

        // 跨设备：不同设备上下文不得消费
        BusinessException crossDevice = assertThrows(BusinessException.class,
                () -> verificationCodeService.verifyCode(userKey, deviceId + "-other",
                        VerificationCodeType.SMS, "111111"));
        assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, crossDevice.getErrorCode());

        // 错误验证码
        BusinessException wrong = assertThrows(BusinessException.class,
                () -> verificationCodeService.verifyCode(userKey, deviceId, VerificationCodeType.SMS, "000000"));
        assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, wrong.getErrorCode());

        // 正确验证码成功消费
        assertDoesNotThrow(() ->
                verificationCodeService.verifyCode(userKey, deviceId, VerificationCodeType.SMS, "111111"));

        // 重复消费
        BusinessException replay = assertThrows(BusinessException.class,
                () -> verificationCodeService.verifyCode(userKey, deviceId, VerificationCodeType.SMS, "111111"));
        assertEquals(CiamErrorCode.VERIFICATION_CODE_INVALID, replay.getErrorCode());
        assertTrue(codeStore.getCode(codeKey).isEmpty(), "消费后验证码必须从 Redis 删除");
    }

    @Test
    @DisplayName("TC-003-3 发送频控在测试环境仍然有效")
    void rateLimit_remainsEffective() {
        String mobile = uniqueMobile();
        String deviceId = "DEV-" + System.nanoTime();
        String userKey = FieldEncryptor.hash(mobile);

        verificationCodeService.sendSmsCode(mobile, "+86", userKey, deviceId);

        BusinessException limited = assertThrows(BusinessException.class,
                () -> verificationCodeService.sendSmsCode(mobile, "+86", userKey, deviceId));
        assertEquals(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED, limited.getErrorCode());
    }
}
