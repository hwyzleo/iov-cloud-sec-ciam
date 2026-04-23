package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.CaptchaAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.CaptchaChallenge;
import org.springframework.stereotype.Service;

/**
 * 图形验证码领域服务。
 * <p>
 * 职责：
 * <ul>
 *   <li>创建验证码挑战并通过 {@link VerificationCodeStore} 记录挑战状态</li>
 *   <li>校验用户提交的验证码答案</li>
 *   <li>密码错误 3 次后由上层认证流程触发挑战</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CaptchaDomainService {

    /** 挑战有效期（秒），5 分钟 */
    static final int CHALLENGE_TTL_SECONDS = 300;

    /** 存储键前缀 */
    static final String KEY_PREFIX = "captcha:";

    private final CaptchaAdapter captchaAdapter;
    private final VerificationCodeStore codeStore;

    /**
     * 创建验证码挑战。
     *
     * @param sessionId 会话标识
     * @return 验证码挑战信息
     */
    public CaptchaChallenge createChallenge(String sessionId) {
        CaptchaChallenge challenge = captchaAdapter.generateChallenge(sessionId);
        // 记录挑战状态，用于后续校验时确认挑战存在且未过期
        codeStore.saveCode(buildKey(challenge.getChallengeId()), "PENDING", CHALLENGE_TTL_SECONDS);
        return challenge;
    }

    /**
     * 校验验证码答案。
     *
     * @param challengeId 挑战唯一标识
     * @param answer      用户提交的答案
     * @throws BusinessException 挑战不存在、已过期或答案错误时抛出 CAPTCHA_INVALID
     */
    public void verifyChallenge(String challengeId, String answer) {
        String key = buildKey(challengeId);

        // 检查挑战是否存在且未过期
        String state = codeStore.getCode(key)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CAPTCHA_INVALID));

        if (!"PENDING".equals(state)) {
            throw new BusinessException(CiamErrorCode.CAPTCHA_INVALID);
        }

        boolean valid = captchaAdapter.verifyChallenge(challengeId, answer);
        // 无论成功失败，挑战只能使用一次
        codeStore.deleteCode(key);

        if (!valid) {
            throw new BusinessException(CiamErrorCode.CAPTCHA_INVALID);
        }
    }

    static String buildKey(String challengeId) {
        return KEY_PREFIX + challengeId;
    }
}
