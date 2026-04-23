package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.PasswordEncoder;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.CredentialType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserCredential;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserCredentialRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * 凭据领域服务 — 封装密码设置、校验、修改、重置逻辑。
 * <p>
 * 首发版本仅支持邮箱密码（{@link CredentialType#EMAIL_PASSWORD}）场景。
 * 密码采用 BCrypt 强哈希存储，不保存明文。
 */
@Service
@RequiredArgsConstructor
public class CredentialDomainService {

    /** 连续失败达到此次数后触发挑战验证 */
    public static final int CHALLENGE_THRESHOLD = 3;
    /** 连续失败达到此次数后锁定账号 */
    public static final int LOCK_THRESHOLD = 5;
    /** 锁定时长（分钟） */
    public static final int LOCK_DURATION_MINUTES = 30;

    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * 为用户设置密码凭据。
     * <p>
     * 若该用户已存在有效的邮箱密码凭据，则抛出异常。
     *
     * @param userId      用户业务唯一标识
     * @param rawPassword 原始密码
     * @return 新创建的凭据数据对象
     */
    public UserCredential setPassword(String userId, String rawPassword) {
        passwordPolicyService.validate(rawPassword);

        // 检查是否已存在有效凭据
        Optional<UserCredential> existing = credentialRepository.findByUserIdAndType(
                userId, CredentialType.EMAIL_PASSWORD.getCode());
        if (existing.isPresent() && existing.get().getCredentialStatus() == CredentialStatus.VALID.getCode()) {
            throw new BusinessException(CiamErrorCode.CREDENTIAL_ALREADY_EXISTS);
        }

        String hash = passwordEncoder.encode(rawPassword);

        UserCredential domain = UserCredential.builder()
                .credentialId(UserIdGenerator.generate())
                .userId(userId)
                .credentialType(CredentialType.EMAIL_PASSWORD.getCode())
                .credentialHash(hash)
                .hashAlgorithm(PasswordEncoder.ALGORITHM)
                .passwordSetTime(DateTimeUtil.getNowInstant())
                .failCount(0)
                .credentialStatus(CredentialStatus.VALID.getCode())
                .build();

        credentialRepository.insert(domain);
        return domain;
    }

    /**
     * 校验用户密码是否匹配，并处理失败计数与锁定逻辑。
     * <p>
     * <ul>
     *   <li>锁定期间直接抛出 ACCOUNT_LOCKED</li>
     *   <li>匹配成功：重置 fail_count 和 locked_until</li>
     *   <li>匹配失败且 fail_count &ge; 5：锁定 30 分钟</li>
     *   <li>匹配失败且 fail_count &ge; 3：返回需要挑战</li>
     * </ul>
     *
     * @param userId      用户业务唯一标识
     * @param rawPassword 原始密码
     * @return 密码校验结果（含挑战/锁定信息）
     * @throws BusinessException 凭据不存在时抛出 CREDENTIAL_INVALID；账号锁定时抛出 ACCOUNT_LOCKED
     */
    public PasswordVerifyResult verifyPassword(String userId, String rawPassword) {
        UserCredential credential = findActiveCredential(userId);

        // 检查是否处于锁定期
        if (isLocked(credential)) {
            throw new BusinessException(CiamErrorCode.ACCOUNT_LOCKED);
        }

        boolean matched = passwordEncoder.matches(rawPassword, credential.getCredentialHash());
        if (matched) {
            credential.setLastVerifyTime(DateTimeUtil.getNowInstant());
            credential.setFailCount(0);
            credential.setLockedUntil(null);
            credentialRepository.updateByCredentialId(credential);
            return PasswordVerifyResult.success();
        }

        // 失败：递增计数
        int newFailCount = (credential.getFailCount() == null ? 0 : credential.getFailCount()) + 1;
        credential.setFailCount(newFailCount);

        if (newFailCount >= LOCK_THRESHOLD) {
            credential.setLockedUntil(DateTimeUtil.getNowInstant().plusSeconds(LOCK_DURATION_MINUTES * 60L));
            credentialRepository.updateByCredentialId(credential);
            return PasswordVerifyResult.failWithLock(newFailCount);
        }

        credentialRepository.updateByCredentialId(credential);

        if (newFailCount >= CHALLENGE_THRESHOLD) {
            return PasswordVerifyResult.failWithChallenge(newFailCount);
        }
        return PasswordVerifyResult.fail(newFailCount);
    }

    /**
     * 修改密码（需验证旧密码）。
     *
     * @param userId         用户业务唯一标识
     * @param oldRawPassword 旧密码
     * @param newRawPassword 新密码
     * @throws BusinessException 旧密码不匹配时抛出 CREDENTIAL_INVALID
     */
    public void changePassword(String userId, String oldRawPassword, String newRawPassword) {
        passwordPolicyService.validate(newRawPassword);

        UserCredential credential = findActiveCredential(userId);

        if (!passwordEncoder.matches(oldRawPassword, credential.getCredentialHash())) {
            throw new BusinessException(CiamErrorCode.CREDENTIAL_INVALID);
        }

        String newHash = passwordEncoder.encode(newRawPassword);
        credential.setCredentialHash(newHash);
        credential.setHashAlgorithm(PasswordEncoder.ALGORITHM);
        credential.setPasswordSetTime(DateTimeUtil.getNowInstant());
        credential.setFailCount(0);
        credentialRepository.updateByCredentialId(credential);
    }

    /**
     * 重置密码（无需旧密码验证，用于忘记密码场景）。
     *
     * @param userId         用户业务唯一标识
     * @param newRawPassword 新密码
     * @throws BusinessException 凭据不存在时抛出 CREDENTIAL_INVALID
     */
    public void resetPassword(String userId, String newRawPassword) {
        passwordPolicyService.validate(newRawPassword);

        UserCredential credential = findActiveCredential(userId);

        String newHash = passwordEncoder.encode(newRawPassword);
        credential.setCredentialHash(newHash);
        credential.setHashAlgorithm(PasswordEncoder.ALGORITHM);
        credential.setPasswordSetTime(DateTimeUtil.getNowInstant());
        credential.setFailCount(0);
        credential.setLockedUntil(null);
        credentialRepository.updateByCredentialId(credential);
    }

    /**
     * 查询用户的有效邮箱密码凭据。
     *
     * @param userId 用户业务唯一标识
     * @return 凭据记录（如存在）
     */
    public Optional<UserCredential> findActiveCredential(String userId, CredentialType type) {
        return credentialRepository.findByUserIdAndType(userId, type.getCode())
                .filter(c -> c.getCredentialStatus() == CredentialStatus.VALID.getCode());
    }

    // ---- 内部方法 ----

    private boolean isLocked(UserCredential credential) {
        Instant lockedUntil = credential.getLockedUntil();
        return lockedUntil != null && DateTimeUtil.getNowInstant().isBefore(lockedUntil);
    }

    private UserCredential findActiveCredential(String userId) {
        return credentialRepository.findByUserIdAndType(userId, CredentialType.EMAIL_PASSWORD.getCode())
                .filter(c -> c.getCredentialStatus() == CredentialStatus.VALID.getCode())
                .orElseThrow(() -> new BusinessException(CiamErrorCode.CREDENTIAL_INVALID));
    }
}
