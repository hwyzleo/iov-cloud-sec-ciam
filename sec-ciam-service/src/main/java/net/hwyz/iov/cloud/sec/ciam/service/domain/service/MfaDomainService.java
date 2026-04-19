package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.TokenDigest;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AdapterResult;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.EmailAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.SmsAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeScene;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamMfaChallengeRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMfaChallengeDo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * MFA 挑战与校验领域服务。
 * <p>
 * 职责：
 * <ul>
 *   <li>创建 MFA 挑战记录并通过短信/邮箱发送验证码</li>
 *   <li>校验验证码，更新挑战状态为通过或失败</li>
 *   <li>取消待验证的挑战</li>
 * </ul>
 * 覆盖场景：新设备登录、异地登录、高风险操作。
 */
@Service
@RequiredArgsConstructor
public class MfaDomainService {

    /** 验证码长度 */
    static final int CODE_LENGTH = 6;
    /** 短信验证码有效期（分钟） */
    static final int SMS_TTL_MINUTES = 5;
    /** 邮箱验证码有效期（分钟） */
    static final int EMAIL_TTL_MINUTES = 30;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CiamMfaChallengeRepository challengeRepository;
    private final SmsAdapter smsAdapter;
    private final EmailAdapter emailAdapter;

    /**
     * 创建 MFA 挑战并发送验证码。
     *
     * @param userId        用户业务唯一标识
     * @param sessionId     会话业务唯一标识（可为 null）
     * @param challengeType 挑战类型（SMS / EMAIL）
     * @param challengeScene 挑战场景
     * @param receiverMask  脱敏接收目标（如 138****1234）
     * @param riskEventId   关联风险事件 ID（可为 null）
     * @return 挑战业务唯一标识 challengeId
     */
    public String createChallenge(String userId, String sessionId,
                                  ChallengeType challengeType, ChallengeScene challengeScene,
                                  String receiverMask, String riskEventId) {
        String challengeId = UserIdGenerator.generate();
        String code = generateCode();
        Instant now = DateTimeUtil.getNowInstant();
        int ttlMinutes = challengeType == ChallengeType.SMS ? SMS_TTL_MINUTES : EMAIL_TTL_MINUTES;
        Instant expireTime = now.plusSeconds(ttlMinutes * 60L);

        CiamMfaChallengeDo challenge = new CiamMfaChallengeDo();
        challenge.setChallengeId(challengeId);
        challenge.setUserId(userId);
        challenge.setSessionId(sessionId);
        challenge.setChallengeType(challengeType.getCode());
        challenge.setChallengeScene(challengeScene.getCode());
        challenge.setReceiverMask(receiverMask);
        challenge.setVerifyCodeHash(TokenDigest.fingerprint(code));
        challenge.setSendTime(now);
        challenge.setExpireTime(expireTime);
        challenge.setChallengeStatus(ChallengeStatus.PENDING.getCode());
        challenge.setRiskEventId(riskEventId);
        challenge.setRowVersion(1);
        challenge.setRowValid(1);
        challenge.setCreateTime(now);
        challenge.setModifyTime(now);
        challengeRepository.insert(challenge);

        // 发送验证码
        sendCode(challengeType, receiverMask, code);

        return challengeId;
    }

    /**
     * 校验 MFA 挑战验证码。
     *
     * @param challengeId 挑战业务唯一标识
     * @param code        用户输入的验证码
     * @return true 表示验证通过，false 表示验证失败
     */
    public boolean verifyChallenge(String challengeId, String code) {
        CiamMfaChallengeDo challenge = challengeRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.MFA_CHALLENGE_NOT_FOUND));

        if (challenge.getChallengeStatus() != ChallengeStatus.PENDING.getCode()) {
            throw new BusinessException(CiamErrorCode.MFA_CHALLENGE_ALREADY_RESOLVED);
        }

        Instant now = DateTimeUtil.getNowInstant();
        if (now.isAfter(challenge.getExpireTime())) {
            challenge.setChallengeStatus(ChallengeStatus.EXPIRED.getCode());
            challenge.setModifyTime(now);
            challengeRepository.updateByChallengeId(challenge);
            throw new BusinessException(CiamErrorCode.MFA_CHALLENGE_EXPIRED);
        }

        boolean matched = TokenDigest.matches(code, challenge.getVerifyCodeHash());
        if (matched) {
            challenge.setChallengeStatus(ChallengeStatus.PASSED.getCode());
            challenge.setVerifyTime(now);
        } else {
            challenge.setChallengeStatus(ChallengeStatus.FAILED.getCode());
        }
        challenge.setModifyTime(now);
        challengeRepository.updateByChallengeId(challenge);
        return matched;
    }

    /**
     * 取消待验证的 MFA 挑战。
     *
     * @param challengeId 挑战业务唯一标识
     */
    public void cancelChallenge(String challengeId) {
        CiamMfaChallengeDo challenge = challengeRepository.findByChallengeId(challengeId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.MFA_CHALLENGE_NOT_FOUND));

        if (challenge.getChallengeStatus() != ChallengeStatus.PENDING.getCode()) {
            throw new BusinessException(CiamErrorCode.MFA_CHALLENGE_ALREADY_RESOLVED);
        }

        challenge.setChallengeStatus(ChallengeStatus.CANCELLED.getCode());
        challenge.setModifyTime(DateTimeUtil.getNowInstant());
        challengeRepository.updateByChallengeId(challenge);
    }

    // ---- 内部方法 ----

    String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int num = RANDOM.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", num);
    }

    private void sendCode(ChallengeType type, String receiver, String code) {
        AdapterResult result;
        if (type == ChallengeType.SMS) {
            result = smsAdapter.sendVerificationCode(receiver, "+86", code);
        } else {
            result = emailAdapter.sendVerificationCode(receiver, code);
        }
        if (!result.isSuccess()) {
            throw new BusinessException(CiamErrorCode.MFA_SEND_FAILED, result.getMessage());
        }
    }
}
