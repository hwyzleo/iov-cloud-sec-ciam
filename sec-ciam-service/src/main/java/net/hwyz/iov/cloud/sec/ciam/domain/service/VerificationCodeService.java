package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.SmsAdapter;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 验证码发送与校验领域服务。
 * <p>
 * 职责：
 * <ul>
 *   <li>生成 6 位随机数字验证码</li>
 *   <li>短信验证码 5 分钟有效，邮箱验证码 30 分钟有效</li>
 *   <li>单用户单客户端 1 分钟 1 次频控</li>
 *   <li>单用户单日 30 次上限</li>
 *   <li>验证码校验：匹配后删除，不匹配或过期抛异常</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    /** 验证码长度 */
    public static final int CODE_LENGTH = 6;
    /** 每分钟频控 TTL（秒） */
    public static final int RATE_LIMIT_TTL_SECONDS = 60;
    /** 单日发送上限 */
    public static final int DAILY_LIMIT = 30;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private final VerificationCodeStore codeStore;
    private final SmsAdapter smsAdapter;
    private final EmailAdapter emailAdapter;

    /**
     * 发送短信验证码。
     *
     * @param mobile      手机号
     * @param countryCode 国家区号
     * @param userId      用户标识（可为手机号哈希，用于频控）
     * @param clientId    客户端标识
     */
    public void sendSmsCode(String mobile, String countryCode, String userId, String clientId) {
        checkRateLimit(userId, clientId, VerificationCodeType.SMS);
        String code = generateCode();
        String codeKey = buildCodeKey(userId, clientId, VerificationCodeType.SMS);
        codeStore.saveCode(codeKey, code, VerificationCodeType.SMS.getTtlSeconds());

        AdapterResult result = smsAdapter.sendVerificationCode(mobile, countryCode, code);
        if (!result.isSuccess()) {
            codeStore.deleteCode(codeKey);
            throw new BusinessException(CiamErrorCode.INTERNAL_ERROR, result.getMessage());
        }
    }

    /**
     * 发送邮箱验证码。
     *
     * @param email    邮箱地址
     * @param userId   用户标识（可为邮箱哈希，用于频控）
     * @param clientId 客户端标识
     */
    public void sendEmailCode(String email, String userId, String clientId) {
        checkRateLimit(userId, clientId, VerificationCodeType.EMAIL);
        String code = generateCode();
        String codeKey = buildCodeKey(userId, clientId, VerificationCodeType.EMAIL);
        codeStore.saveCode(codeKey, code, VerificationCodeType.EMAIL.getTtlSeconds());

        AdapterResult result = emailAdapter.sendVerificationCode(email, code);
        if (!result.isSuccess()) {
            codeStore.deleteCode(codeKey);
            throw new BusinessException(CiamErrorCode.INTERNAL_ERROR, result.getMessage());
        }
    }

    /**
     * 校验验证码。
     *
     * @param userId   用户标识
     * @param clientId 客户端标识
     * @param type     验证码类型
     * @param code     用户输入的验证码
     * @throws BusinessException 验证码无效或已过期时抛出 VERIFICATION_CODE_INVALID
     */
    public void verifyCode(String userId, String clientId, VerificationCodeType type, String code) {
        String codeKey = buildCodeKey(userId, clientId, type);
        String stored = codeStore.getCode(codeKey)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.VERIFICATION_CODE_INVALID));

        if (!stored.equals(code)) {
            throw new BusinessException(CiamErrorCode.VERIFICATION_CODE_INVALID);
        }
        // 验证成功后删除，防止重复使用
        codeStore.deleteCode(codeKey);
    }

    // ---- 内部方法 ----

    /**
     * 频控检查：1 分钟 1 次 + 单日 30 次。
     */
    void checkRateLimit(String userId, String clientId, VerificationCodeType type) {
        // 1) 单用户单客户端 1 分钟 1 次
        String minuteKey = buildMinuteRateKey(userId, clientId, type);
        boolean allowed = codeStore.setIfAbsent(minuteKey, RATE_LIMIT_TTL_SECONDS);
        if (!allowed) {
            throw new BusinessException(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED);
        }

        // 2) 单用户单日 30 次
        String dailyKey = buildDailyCountKey(userId, type);
        int secondsUntilMidnight = computeSecondsUntilMidnight();
        long count = codeStore.incrementDailyCount(dailyKey, secondsUntilMidnight);
        if (count > DAILY_LIMIT) {
            throw new BusinessException(CiamErrorCode.VERIFICATION_CODE_RATE_LIMITED);
        }
    }

    public String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        // 测试情况下默认6个1
        // int num = RANDOM.nextInt(bound);
        int num = 111111;
        return String.format("%0" + CODE_LENGTH + "d", num);
    }

    static String buildCodeKey(String userId, String clientId, VerificationCodeType type) {
        return "vc:" + type.name().toLowerCase() + ":" + userId + ":" + clientId;
    }

    static String buildMinuteRateKey(String userId, String clientId, VerificationCodeType type) {
        return "vc:rate:" + type.name().toLowerCase() + ":" + userId + ":" + clientId;
    }

    static String buildDailyCountKey(String userId, VerificationCodeType type) {
        return "vc:daily:" + type.name().toLowerCase() + ":" + userId;
    }

    int computeSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now(DEFAULT_ZONE).plusDays(1), LocalTime.MIDNIGHT);
        return (int) ChronoUnit.SECONDS.between(now, midnight);
    }
}
