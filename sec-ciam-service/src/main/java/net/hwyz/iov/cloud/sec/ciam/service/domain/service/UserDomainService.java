package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RegisterSource;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import org.springframework.stereotype.Service;

/**
 * 用户领域服务 — 封装用户主档创建逻辑。
 * <p>
 * 业务侧禁止直接拼装 {@link User} 或 {@link UserProfile}，
 * 必须通过本服务完成用户创建，以保证：
 * <ul>
 *   <li>全局唯一 user_id 由 {@link UserIdGenerator} 统一生成</li>
 *   <li>初始状态固定为 {@link UserStatus#PENDING}（待验证）</li>
 *   <li>用户主表与资料扩展表同步创建</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final CiamUserRepository userRepository;
    private final CiamUserProfileRepository userProfileRepository;

    /**
     * 创建用户主档（含资料扩展记录）。
     *
     * @param registerSource       注册来源
     * @param registerChannel       注册渠道（可为 null）
     * @param brandCode            品牌编码（可为 null，默认 OPENIOV）
     * @param primaryIdentityType   主要身份类型（可为 null）
     * @return 新创建的用户数据对象
     */
    public User createUser(RegisterSource registerSource,
                                 String registerChannel,
                                 String brandCode,
                                 IdentityType primaryIdentityType) {
        String userId = UserIdGenerator.generate();
        String effectiveBrand = (brandCode == null || brandCode.isBlank())
                ? "OPENIOV" : brandCode;

        // 构建用户主表记录
        User user = User.builder()
                .userId(userId)
                .userStatus(UserStatus.PENDING.getCode())
                .brandCode(effectiveBrand)
                .registerSource(registerSource.getCode())
                .registerChannel(registerChannel)
                .primaryIdentityType(primaryIdentityType != null ? primaryIdentityType.getCode() : null)
                .build();
        userRepository.insert(user);

        // 同步创建用户资料扩展记录
        UserProfile profile = UserProfile.builder()
                .profileId(UserIdGenerator.generate())
                .userId(userId)
                .gender(0) // 未知
                .build();
        userProfileRepository.insert(profile);

        return user;
    }

    // ---- 状态流转方法 ----

    /**
     * 激活用户（PENDING → ACTIVE）。
     */
    public void activate(String userId) {
        transitStatus(userId, UserStatus.ACTIVE);
    }

    /**
     * 锁定用户（ACTIVE → LOCKED）。
     */
    public void lock(String userId) {
        transitStatus(userId, UserStatus.LOCKED);
    }

    /**
     * 解锁用户（LOCKED → ACTIVE）。
     */
    public void unlock(String userId) {
        transitStatus(userId, UserStatus.ACTIVE);
    }

    /**
     * 禁用用户（ACTIVE → DISABLED）。
     */
    public void disable(String userId) {
        transitStatus(userId, UserStatus.DISABLED);
    }

    /**
     * 启用用户（DISABLED → ACTIVE）。
     */
    public void enable(String userId) {
        transitStatus(userId, UserStatus.ACTIVE);
    }

    /**
     * 发起注销（ACTIVE → DEACTIVATING）。
     */
    public void startDeactivation(String userId) {
        transitStatus(userId, UserStatus.DEACTIVATING);
    }

    /**
     * 完成注销（DEACTIVATING → DEACTIVATED）。
     */
    public void completeDeactivation(String userId) {
        transitStatus(userId, UserStatus.DEACTIVATED);
    }

    /**
     * 取消注销（DEACTIVATING → ACTIVE）。
     */
    public void cancelDeactivation(String userId) {
        transitStatus(userId, UserStatus.ACTIVE);
    }

    /**
     * 根据用户 ID 查询用户主档。
     *
     * @param userId 用户业务唯一标识
     * @return 用户数据对象（可能为空）
     */
    public java.util.Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }


    // ---- 内部方法 ----

    private User findUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));
    }

    private void transitStatus(String userId, UserStatus target) {
        User user = findUser(userId);
        UserStatus current = UserStatus.fromCode(user.getUserStatus());
        UserStatusMachine.validateTransition(current, target);
        user.setUserStatus(target.getCode());
        userRepository.updateByUserId(user);
    }
}
