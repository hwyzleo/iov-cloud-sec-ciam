package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeScene;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.ChallengeType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.MfaDomainService;
import org.springframework.stereotype.Service;

/**
 * MFA 应用服务
 */
@Service
@RequiredArgsConstructor
public class MfaAppService {

    private final MfaDomainService mfaDomainService;

    /**
     * 触发 MFA 挑战
     */
    public String triggerMfa(String userId, String sessionId, String challengeType, String challengeScene, String receiverMask, String riskEventId) {
        return mfaDomainService.createChallenge(userId, sessionId,
                ChallengeType.fromCode(challengeType),
                ChallengeScene.fromCode(challengeScene),
                receiverMask, riskEventId);
    }

    /**
     * 校验 MFA 挑战
     */
    public boolean verifyMfa(String challengeId, String code) {
        return mfaDomainService.verifyChallenge(challengeId, code);
    }

}
