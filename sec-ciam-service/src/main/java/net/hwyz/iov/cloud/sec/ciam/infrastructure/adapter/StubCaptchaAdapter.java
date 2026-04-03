package net.hwyz.iov.cloud.sec.ciam.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.CaptchaAdapter;
import net.hwyz.iov.cloud.sec.ciam.domain.adapter.CaptchaChallenge;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码适配器 — 桩实现（开发环境使用）。
 * <p>
 * 生成简单的数学运算验证码（如 "3 + 5 = ?"），答案存储在内存中。
 * 生产环境应替换为真实验证码服务商实现。
 */
@Slf4j
@Component
public class StubCaptchaAdapter implements CaptchaAdapter {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_OPERAND = 20;

    /** 内存存储挑战答案，仅用于开发环境 */
    private final Map<String, String> answerStore = new ConcurrentHashMap<>();

    @Override
    public CaptchaChallenge generateChallenge(String sessionId) {
        int a = RANDOM.nextInt(MAX_OPERAND) + 1;
        int b = RANDOM.nextInt(MAX_OPERAND) + 1;
        int result = a + b;
        String challengeId = UUID.randomUUID().toString();
        String question = a + " + " + b + " = ?";

        answerStore.put(challengeId, String.valueOf(result));
        log.info("[CAPTCHA-STUB] 生成数学验证码: sessionId={}, challengeId={}, question={}, answer={}",
                sessionId, challengeId, question, result);

        return CaptchaChallenge.builder()
                .challengeId(challengeId)
                .challengeType(CaptchaChallenge.CaptchaType.IMAGE)
                .challengeData(question)
                .build();
    }

    @Override
    public boolean verifyChallenge(String challengeId, String answer) {
        String expected = answerStore.remove(challengeId);
        if (expected == null) {
            log.warn("[CAPTCHA-STUB] 挑战不存在或已过期: challengeId={}", challengeId);
            return false;
        }
        boolean matched = expected.equals(answer);
        log.info("[CAPTCHA-STUB] 校验验证码: challengeId={}, expected={}, answer={}, matched={}",
                challengeId, expected, answer, matched);
        return matched;
    }

    /** 清空所有挑战数据（测试辅助） */
    public void clear() {
        answerStore.clear();
    }
}
