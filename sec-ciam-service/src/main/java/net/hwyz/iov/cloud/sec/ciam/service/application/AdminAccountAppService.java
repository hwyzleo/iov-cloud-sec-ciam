package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RegisterSource;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserCredentialRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserIdentityRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserProfileRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 运营后台账号管理应用服务 — 账号的增删改查。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountAppService {

    private final CiamUserRepository userRepository;
    private final CiamUserIdentityRepository identityRepository;
    private final CiamUserProfileRepository profileRepository;
    private final CiamUserCredentialRepository credentialRepository;
    private final AuditLogger auditLogger;
    private final FieldEncryptor fieldEncryptor;

    /**
     * 创建账号。
     *
     * @param identityType   身份类型：MOBILE, EMAIL
     * @param identityValue  身份值（手机号或邮箱）
     * @param password       密码
     * @param nickname       昵称
     * @param gender         性别
     * @param registerSource 注册来源
     * @param enabled        是否启用
     * @param remark         备注
     * @param adminId        管理员ID
     * @return 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String createAccount(String identityType, String identityValue, String password,
                                 String nickname, Integer gender, String registerSource,
                                 Boolean enabled, String remark, String adminId) {
        Optional<CiamUserIdentityDo> existing = identityRepository.findByTypeAndValue(
                identityType, identityValue);
        if (existing.isPresent()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, identityType + "已存在");
        }

        if (registerSource == null || registerSource.isEmpty()) {
            registerSource = "MOBILE".equalsIgnoreCase(identityType)
                    ? RegisterSource.ADMIN_MOBILE.getCode()
                    : RegisterSource.ADMIN_EMAIL.getCode();
        }

        String userId = UserIdGenerator.generate();
        CiamUserDo user = new CiamUserDo();
        user.setUserId(userId);
        user.setUserStatus(UserStatus.PENDING.getCode());
        user.setRegisterSource(registerSource);
        user.setPrimaryIdentityType(identityType);
        user.setDescription(remark);
        user.setCreateTime(DateTimeUtil.getNowInstant());
        user.setCreateBy(adminId);
        user.setModifyTime(DateTimeUtil.getNowInstant());
        user.setModifyBy(adminId);
        user.setRowVersion(1);
        user.setRowValid(1);
        userRepository.insert(user);

        CiamUserIdentityDo identity = new CiamUserIdentityDo();
        identity.setIdentityId(UserIdGenerator.generate());
        identity.setUserId(userId);
        identity.setIdentityType(identityType);
        identity.setIdentityValue(fieldEncryptor.encrypt(identityValue));
        identity.setIdentityHash(FieldEncryptor.hash(identityValue));
        identity.setVerifiedFlag(0);
        identity.setCreateTime(DateTimeUtil.getNowInstant());
        identity.setCreateBy(adminId);
        identity.setModifyTime(DateTimeUtil.getNowInstant());
        identity.setModifyBy(adminId);
        identity.setRowVersion(1);
        identity.setRowValid(1);
        identityRepository.insert(identity);

        CiamUserProfileDo profile = new CiamUserProfileDo();
        profile.setProfileId(UserIdGenerator.generate());
        profile.setUserId(userId);
        profile.setNickname(nickname);
        profile.setGender(gender);
        profile.setCreateTime(DateTimeUtil.getNowInstant());
        profile.setCreateBy(adminId);
        profile.setModifyTime(DateTimeUtil.getNowInstant());
        profile.setModifyBy(adminId);
        profile.setRowVersion(1);
        profile.setRowValid(1);
        profileRepository.insert(profile);

        log.info("管理员创建账号: userId={}, identityType={}, identityValue={}, adminId={}",
                userId, identityType, identityValue, adminId);
        return userId;
    }

    /**
     * 更新账号。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(String userId, String identityType, String identityValue,
                               String nickname, Integer gender, Boolean enabled,
                               String remark, String adminId) {
        CiamUserDo user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        if (enabled != null) {
            user.setUserStatus(enabled ? UserStatus.ACTIVE.getCode() : UserStatus.DISABLED.getCode());
        }
        if (remark != null) {
            user.setDescription(remark);
        }
        user.setModifyTime(DateTimeUtil.getNowInstant());
        user.setModifyBy(adminId);
        userRepository.updateByUserId(user);

        if (identityType != null && identityValue != null) {
            Optional<CiamUserIdentityDo> existingIdentity = identityRepository.findByTypeAndValue(
                    identityType, identityValue);
            if (existingIdentity.isPresent() && !existingIdentity.get().getUserId().equals(userId)) {
                throw new BusinessException(CiamErrorCode.INVALID_PARAM, identityType + "已被其他用户使用");
            }
            String hashedValue = FieldEncryptor.hash(identityValue);
            String encryptedValue = fieldEncryptor.encrypt(identityValue);
            identityRepository.updateIdentityValue(userId, identityType, hashedValue, encryptedValue);
        }

        Optional<CiamUserProfileDo> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            CiamUserProfileDo profile = profileOpt.get();
            if (nickname != null) {
                profile.setNickname(nickname);
            }
            if (gender != null) {
                profile.setGender(gender);
            }
            profile.setModifyTime(DateTimeUtil.getNowInstant());
            profile.setModifyBy(adminId);
            profileRepository.updateByUserId(profile);
        }

        log.info("管理员更新账号: userId={}, adminId={}", userId, adminId);
    }

    /**
     * 删除账号（物理删除，删除所有相关数据）。
     *
     * @param userId   用户ID
     * @param adminId  管理员ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(String userId, String adminId) {
        CiamUserDo user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        identityRepository.physicalDeleteByUserId(userId);
        profileRepository.physicalDeleteByUserId(userId);
        credentialRepository.physicalDeleteByUserId(userId);
        userRepository.physicalDeleteByUserId(userId);

        log.info("管理员删除账号: userId={}, adminId={}", userId, adminId);
    }

    /**
     * 批量删除账号（物理删除，删除所有相关数据）。
     *
     * @param userIds  用户ID列表
     * @param adminId  管理员ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccounts(List<String> userIds, String adminId) {
        for (String userId : userIds) {
            identityRepository.physicalDeleteByUserId(userId);
            profileRepository.physicalDeleteByUserId(userId);
            credentialRepository.physicalDeleteByUserId(userId);
            userRepository.physicalDeleteByUserId(userId);
            log.info("管理员删除账号: userId={}, adminId={}", userId, adminId);
        }
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success, String operator) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .requestSnapshot(operator != null ? "operator=" + operator : null)
                .build());
    }
}
