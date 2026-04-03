package net.hwyz.iov.cloud.sec.ciam.domain.service;

import lombok.Getter;

/**
 * 密码校验结果。
 * <p>
 * 除了匹配与否，还携带是否需要挑战验证、当前失败次数等信息，
 * 供上层认证流程决定是否触发图形验证码或 MFA。
 */
@Getter
public class PasswordVerifyResult {

    private final boolean matched;
    private final boolean challengeRequired;
    private final boolean locked;
    private final int failCount;

    private PasswordVerifyResult(boolean matched, boolean challengeRequired, boolean locked, int failCount) {
        this.matched = matched;
        this.challengeRequired = challengeRequired;
        this.locked = locked;
        this.failCount = failCount;
    }

    /** 验证成功 */
    public static PasswordVerifyResult success() {
        return new PasswordVerifyResult(true, false, false, 0);
    }

    /** 验证失败，需要挑战（fail_count >= 3 且 < 5） */
    public static PasswordVerifyResult failWithChallenge(int failCount) {
        return new PasswordVerifyResult(false, true, false, failCount);
    }

    /** 验证失败，账号已锁定（fail_count >= 5） */
    public static PasswordVerifyResult failWithLock(int failCount) {
        return new PasswordVerifyResult(false, true, true, failCount);
    }

    /** 验证失败，尚未触发挑战（fail_count < 3） */
    public static PasswordVerifyResult fail(int failCount) {
        return new PasswordVerifyResult(false, false, false, failCount);
    }
}
