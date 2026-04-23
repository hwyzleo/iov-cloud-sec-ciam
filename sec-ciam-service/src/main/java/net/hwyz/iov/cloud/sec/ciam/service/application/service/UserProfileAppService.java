package net.hwyz.iov.cloud.sec.ciam.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.UserProfileAssembler;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.UserProfileDto;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.common.util.UserIdGenerator;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserProfile;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

/**
 * 用户资料应用服务 — 编排用户资料查询与维护。
 * <p>
 * 职责：
 * <ul>
 *   <li>查询用户资料</li>
 *   <li>更新可编辑字段（昵称、头像、性别、生日、地区）</li>
 *   <li>敏感字段变更需安全校验（如姓名）</li>
 *   <li>注册时创建默认资料</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileAppService {

    private final UserProfileRepository profileRepository;
    private final AuditLogger auditLogger;

    @Value("${biz.default-avatar}")
    private String defaultAvatar;

    /**
     * 敏感字段集合，变更时需要安全校验
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of("realName");

    /**
     * 查询用户资料。
     *
     * @param userId 用户业务唯一标识
     * @return 用户资料数据对象
     */
    public UserProfileDto getProfile(String userId) {
        UserProfile userProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.PROFILE_NOT_FOUND));
        if (StrUtil.isBlank(userProfile.getAvatarUrl())) {
            userProfile.setAvatarUrl(defaultAvatar);
        }
        return UserProfileAssembler.INSTANCE.toDto(userProfile);
    }

    /**
     * 更新可编辑的资料字段。
     * <p>
     * 支持更新：nickname, avatarUrl, gender, birthday, regionCode, regionName。
     * 不支持通过此方法更新敏感字段（如 realName）。
     *
     * @param userId     用户业务唯一标识
     * @param nickname   昵称（null 表示不更新）
     * @param avatarUrl  头像地址（null 表示不更新）
     * @param gender     性别（null 表示不更新）
     * @param birthday   生日（null 表示不更新）
     * @param regionCode 地区编码（null 表示不更新）
     * @param regionName 地区名称（null 表示不更新）
     */
    public void updateProfile(String userId, String nickname, String avatarUrl,
                              Integer gender, LocalDate birthday,
                              String regionCode, String regionName) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.PROFILE_NOT_FOUND));

        if (nickname != null) {
            profile.setNickname(nickname);
        }
        if (avatarUrl != null) {
            profile.setAvatarUrl(avatarUrl);
        }
        if (gender != null) {
            profile.setGender(gender);
        }
        if (birthday != null) {
            profile.setBirthday(birthday);
        }
        if (regionCode != null) {
            profile.setRegionCode(regionCode);
        }
        if (regionName != null) {
            profile.setRegionName(regionName);
        }

        profileRepository.updateByProfileId(profile);

        logAudit(userId, AuditEventType.PROFILE_UPDATE, true);
        log.info("用户资料更新: userId={}", userId);
    }

    /**
     * 更新敏感字段（需安全校验）。
     * <p>
     * 敏感字段（如 realName）变更前需要验证 verificationToken。
     *
     * @param userId            用户业务唯一标识
     * @param field             字段名称
     * @param value             新值
     * @param verificationToken 安全校验令牌
     */
    public void updateSensitiveField(String userId, String field, String value,
                                     String verificationToken) {
        if (!SENSITIVE_FIELDS.contains(field)) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM,
                    "字段 " + field + " 不属于敏感字段");
        }
        if (verificationToken == null || verificationToken.isBlank()) {
            throw new BusinessException(CiamErrorCode.SENSITIVE_FIELD_VERIFICATION_REQUIRED);
        }

        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.PROFILE_NOT_FOUND));

        if ("realName".equals(field)) {
            profile.setRealName(value);
        }

        profileRepository.updateByProfileId(profile);

        logAudit(userId, AuditEventType.PROFILE_SENSITIVE_UPDATE, true);
        log.info("敏感字段更新: userId={}, field={}", userId, field);
    }

    /**
     * 注册时创建默认资料（如果不存在）。
     *
     * @param userId 用户业务唯一标识
     */
    public void createProfileIfNotExists(String userId) {
        if (profileRepository.findByUserId(userId).isPresent()) {
            log.debug("用户资料已存在，跳过创建: userId={}", userId);
            return;
        }

        UserProfile profile = UserProfile.builder()
                .profileId(UserIdGenerator.generate())
                .userId(userId)
                .gender(0)
                .build();
        profileRepository.insert(profile);

        log.info("创建默认用户资料: userId={}", userId);
    }

    private void logAudit(String userId, AuditEventType eventType, boolean success) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .build());
    }
}
