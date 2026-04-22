package net.hwyz.iov.cloud.sec.ciam.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.LoginResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEvent;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditEventType;
import net.hwyz.iov.cloud.sec.ciam.service.common.audit.AuditLogger;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;
import net.hwyz.iov.cloud.sec.ciam.service.common.security.FieldEncryptor;
import net.hwyz.iov.cloud.framework.common.util.DateTimeUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.AppleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.CaptchaChallenge;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.GoogleLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.LocalMobileAuthAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.ThirdPartyUserInfo;
import net.hwyz.iov.cloud.sec.ciam.service.domain.adapter.WechatLoginAdapter;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.RegisterSource;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.UserStatus;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.User;
import net.hwyz.iov.cloud.sec.ciam.service.domain.model.UserIdentity;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamUserRepository;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * 认证应用服务 — 编排注册/登录主链路。
 * <p>
 * 职责：
 * <ul>
 *   <li>发送手机/邮箱验证码</li>
 *   <li>手机号验证码登录（账号不存在自动注册，存在直接登录）</li>
 *   <li>邮箱密码登录（含挑战与锁定逻辑）</li>
 *   <li>邮箱验证码登录（账号不存在自动注册）</li>
 *   <li>微信、Apple、Google 第三方登录（首次自动注册，冲突进入合并流程）</li>
 *   <li>记录审计日志</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationAppService {

    private final VerificationCodeService verificationCodeService;
    private final IdentityDomainService identityDomainService;
    private final UserDomainService userDomainService;
    private final CiamUserRepository userRepository;
    private final AuditLogger auditLogger;
    private final CredentialDomainService credentialDomainService;
    private final CaptchaDomainService captchaDomainService;
    private final SessionDomainService sessionDomainService;
    private final WechatLoginAdapter wechatLoginAdapter;
    private final AppleLoginAdapter appleLoginAdapter;
    private final GoogleLoginAdapter googleLoginAdapter;
    private final LocalMobileAuthAdapter localMobileAuthAdapter;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenDomainService refreshTokenDomainService;
    private final DeviceDomainService deviceDomainService;

    /**
     * 发送手机验证码。
     *
     * @param mobile      手机号
     * @param countryCode 国家区号
     * @param deviceId    设备标识
     */
    public void sendMobileVerificationCode(String mobile, String countryCode, String deviceId) {
        String userKey = FieldEncryptor.hash(mobile);
        verificationCodeService.sendSmsCode(mobile, countryCode, userKey, deviceId);
    }

    /**
     * 手机号验证码登录（账号不存在自动注册）。
     *
     * @param mobile      手机号
     * @param countryCode 国家区号
     * @param code        验证码
     * @param deviceId    设备标识
     * @param deviceInfo  设备信息
     * @return 登录结果
     */
    public LoginResultDto loginByMobileCode(String mobile, String countryCode,
                                            String code, String deviceId,
                                            DeviceInfoDto deviceInfo) {
        String userKey = FieldEncryptor.hash(mobile);

        // 1. 校验验证码
        try {
            verificationCodeService.verifyCode(userKey, deviceId, VerificationCodeType.SMS, code);
        } catch (BusinessException e) {
            logAudit(null, deviceId, AuditEventType.LOGIN_FAIL, false);
            throw e;
        }

        // 2. 查找手机号对应的登录标识
        Optional<UserIdentity> identityOpt =
                identityDomainService.findByTypeAndValue(IdentityType.MOBILE, mobile);

        try {
            if (identityOpt.isPresent()
                    && identityOpt.get().getIdentityStatus().equals(net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus.BOUND.getCode())) {
                String userId = identityOpt.get().getUserId();
                deviceDomainService.recordDevice(userId, deviceId, deviceInfo);
                return handleExistingUserLogin(identityOpt.get(), deviceId, deviceInfo.getClientId());
            } else {
                return handleNewUserRegistration(mobile, countryCode, deviceId, deviceInfo);
            }
        } catch (BusinessException e) {
            logAudit(identityOpt.map(UserIdentity::getUserId).orElse(null),
                    deviceId, AuditEventType.LOGIN_FAIL, false);
            throw e;
        }
    }

    /**
     * 发送邮箱验证码。
     *
     * @param email    邮箱地址
     * @param deviceId 设备标识
     */
    public void sendEmailVerificationCode(String email, String deviceId) {
        String userKey = FieldEncryptor.hash(email);
        verificationCodeService.sendEmailCode(email, userKey, deviceId);
    }

    /**
     * 邮箱密码登录。
     * <p>
     * 流程：查找邮箱标识 → 校验账号状态 → 校验密码 → 处理挑战/锁定 → 返回结果。
     *
     * @param email         邮箱地址
     * @param password      密码
     * @param clientId      客户端标识
     * @param captchaId     图形验证码挑战 ID（可为 null）
     * @param captchaAnswer 图形验证码答案（可为 null）
     * @return 登录结果
     */
    public LoginResultDto loginByEmailPassword(String email, String password, String clientId,
                                               String captchaId, String captchaAnswer) {
        // 1. 查找邮箱标识
        Optional<UserIdentity> identityOpt =
                identityDomainService.findByTypeAndValue(IdentityType.EMAIL, email);

        if (identityOpt.isEmpty()
                || identityOpt.get().getIdentityStatus() != IdentityStatus.BOUND.getCode()) {
            logAudit(null, clientId, AuditEventType.LOGIN_FAIL, false);
            throw new BusinessException(CiamErrorCode.CREDENTIAL_INVALID);
        }

        UserIdentity identity = identityOpt.get();
        String userId = identity.getUserId();

        // 2. 校验账号状态
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    logAudit(userId, clientId, AuditEventType.LOGIN_FAIL, false);
                    return new BusinessException(CiamErrorCode.CREDENTIAL_INVALID);
                });

        UserStatus status = UserStatus.fromCode(user.getUserStatus());
        if (status == UserStatus.LOCKED) {
            logAudit(userId, clientId, AuditEventType.LOGIN_FAIL, false);
            throw new BusinessException(CiamErrorCode.ACCOUNT_LOCKED);
        }
        if (status != UserStatus.ACTIVE) {
            logAudit(userId, clientId, AuditEventType.LOGIN_FAIL, false);
            throw new BusinessException(CiamErrorCode.ACCOUNT_DISABLED);
        }

        // 3. 校验密码
        PasswordVerifyResult verifyResult = credentialDomainService.verifyPassword(userId, password);

        // 4. 处理锁定
        if (verifyResult.isLocked()) {
            logAudit(userId, clientId, AuditEventType.ACCOUNT_LOCK, false);
            throw new BusinessException(CiamErrorCode.ACCOUNT_LOCKED);
        }

        // 5. 处理挑战
        if (verifyResult.isChallengeRequired() && !verifyResult.isMatched()) {
            // 密码错误且需要挑战 → 返回挑战
            CaptchaChallenge challenge = captchaDomainService.createChallenge(userId);
            logAudit(userId, clientId, AuditEventType.LOGIN_FAIL, false);
            return LoginResultDto.builder()
                    .challengeRequired(true)
                    .captchaChallenge(challenge)
                    .build();
        }

        if (!verifyResult.isMatched()) {
            // 密码错误，尚未触发挑战
            logAudit(userId, clientId, AuditEventType.LOGIN_FAIL, false);
            throw new BusinessException(CiamErrorCode.CREDENTIAL_INVALID);
        }

        // 6. 密码匹配 — 如果之前有挑战要求，需要验证 captcha
        // verifyPassword 成功时 challengeRequired=false，但如果调用方提供了 captcha 则验证
        if (captchaId != null && captchaAnswer != null) {
            captchaDomainService.verifyChallenge(captchaId, captchaAnswer);
        }

        // 7. 登录成功
        logAudit(userId, clientId, AuditEventType.LOGIN_SUCCESS, true);
        return LoginResultDto.builder()
                .userId(userId)
                .newUser(false)
                .build();
    }

    /**
     * 邮箱验证码登录（账号不存在自动注册）。
     *
     * @param email    邮箱地址
     * @param code     验证码
     * @param clientId 客户端标识
     * @return 登录结果
     */
    public LoginResultDto loginByEmailCode(String email, String code, String clientId) {
        String userKey = FieldEncryptor.hash(email);

        // 1. 校验验证码
        try {
            verificationCodeService.verifyCode(userKey, clientId, VerificationCodeType.EMAIL, code);
        } catch (BusinessException e) {
            logAudit(null, clientId, AuditEventType.LOGIN_FAIL, false);
            throw e;
        }

        // 2. 查找邮箱标识
        Optional<UserIdentity> identityOpt =
                identityDomainService.findByTypeAndValue(IdentityType.EMAIL, email);

        try {
            if (identityOpt.isPresent()
                    && identityOpt.get().getIdentityStatus().equals(net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus.BOUND.getCode())) {
                return handleExistingUserLogin(identityOpt.get(), clientId, null);
            } else {
                return handleNewEmailUserRegistration(email, clientId);
            }
        } catch (BusinessException e) {
            logAudit(identityOpt.map(UserIdentity::getUserId).orElse(null),
                    clientId, AuditEventType.LOGIN_FAIL, false);
            throw e;
        }
    }

    // ---- 第三方登录 ----

    /**
     * 微信登录。
     * <p>
     * 使用微信授权码换取用户信息，根据 openId 查找已绑定标识：
     * 已绑定 → 直接登录；未绑定 → 自动注册；冲突 → 抛出合并待处理异常。
     *
     * @param code     微信授权码
     * @param clientId 客户端标识
     * @return 登录结果
     */
    public LoginResultDto loginByWechat(String code, String clientId) {
        ThirdPartyUserInfo userInfo = wechatLoginAdapter.getUserInfo(code);
        return handleThirdPartyLogin(userInfo, IdentityType.WECHAT, clientId, "wechat_login");
    }

    /**
     * Apple 登录。
     * <p>
     * 验证 Apple identity token，根据 subject 查找已绑定标识：
     * 已绑定 → 直接登录；未绑定 → 自动注册；冲突 → 抛出合并待处理异常。
     *
     * @param identityToken Apple 签发的 identity token
     * @param clientId      客户端标识
     * @return 登录结果
     */
    public LoginResultDto loginByApple(String identityToken, String clientId) {
        ThirdPartyUserInfo userInfo = appleLoginAdapter.verifyIdentityToken(identityToken);
        return handleThirdPartyLogin(userInfo, IdentityType.APPLE, clientId, "apple_login");
    }

    /**
     * Google 登录。
     * <p>
     * 验证 Google ID token，根据 subject 查找已绑定标识：
     * 已绑定 → 直接登录；未绑定 → 自动注册；冲突 → 抛出合并待处理异常。
     *
     * @param idToken  Google 签发的 ID token
     * @param clientId 客户端标识
     * @return 登录结果
     */
    public LoginResultDto loginByGoogle(String idToken, String clientId) {
        ThirdPartyUserInfo userInfo = googleLoginAdapter.verifyIdToken(idToken);
        return handleThirdPartyLogin(userInfo, IdentityType.GOOGLE, clientId, "google_login");
    }

    /**
     * 本机手机号登录。
     * <p>
     * 使用运营商本机号码认证 token 获取手机号，根据手机号查找已绑定标识：
     * 已绑定 → 直接登录；未绑定 → 自动注册并绑定标识。
     * 若适配器返回 null（环境不支持），则返回 fallbackRequired=true，提示客户端回退到短信验证码登录。
     *
     * @param token      运营商认证 token
     * @param clientId   客户端标识
     * @param deviceInfo 设备信息
     * @return 登录结果
     */
    public LoginResultDto loginByLocalMobile(String token, String clientId, DeviceInfoDto deviceInfo) {
        // 1. 调用本机号码认证适配器
        String mobile = localMobileAuthAdapter.verifyToken(token);

        // 2. 适配器返回 null → 环境不支持，回退到短信验证码登录
        if (mobile == null) {
            log.info("本机手机号认证不可用，需回退到短信验证码登录: clientId={}", clientId);
            logAudit(null, clientId, AuditEventType.LOGIN_FAIL, false);
            return LoginResultDto.builder()
                    .fallbackRequired(true)
                    .build();
        }

        // 3. 查找手机号对应的登录标识
        Optional<UserIdentity> identityOpt =
                identityDomainService.findByTypeAndValue(IdentityType.MOBILE, mobile);

        try {
            if (identityOpt.isPresent()
                    && identityOpt.get().getIdentityStatus().equals(net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus.BOUND.getCode())) {
                String userId = identityOpt.get().getUserId();
                deviceDomainService.recordDevice(userId, clientId, deviceInfo);
                return handleExistingUserLogin(identityOpt.get(), clientId, deviceInfo.getClientId());
            } else {
                return handleNewLocalMobileUserRegistration(mobile, clientId, deviceInfo);
            }
        } catch (BusinessException e) {
            logAudit(identityOpt.map(UserIdentity::getUserId).orElse(null),
                    clientId, AuditEventType.LOGIN_FAIL, false);
            throw e;
        }
    }

    // ---- 退出登录 ----

    /**
     * 退出登录。
     * <p>
     * 将当前会话设为下线状态，撤销关联的 Refresh Token，并记录审计日志。
     *
     * @param sessionId 会话业务唯一标识
     * @param userId    用户业务唯一标识
     * @param clientId  客户端标识
     */
    public void logout(String sessionId, String userId, String clientId) {
        sessionDomainService.logout(sessionId, userId);
        logAudit(userId, clientId, AuditEventType.LOGOUT, true);
    }

    // ---- 内部方法 ----

    private LoginResultDto handleExistingUserLogin(UserIdentity identity, String deviceId, String clientId) {
        String userId = identity.getUserId();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.USER_NOT_FOUND));

        UserStatus status = UserStatus.fromCode(user.getUserStatus());
        if (status == UserStatus.LOCKED) {
            throw new BusinessException(CiamErrorCode.ACCOUNT_LOCKED);
        }
        if (status != UserStatus.ACTIVE) {
            throw new BusinessException(CiamErrorCode.ACCOUNT_DISABLED);
        }

        logAudit(userId, deviceId, AuditEventType.LOGIN_SUCCESS, true);

        int accessTokenTtl = 1800;
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtTokenService.generateAccessToken(
                userId, deviceId, "default", sessionId, accessTokenTtl);
        String refreshToken = refreshTokenDomainService.issueRefreshToken(
                userId, sessionId, clientId, 7 * 24 * 60 * 60);

        return LoginResultDto.builder()
                .userId(userId)
                .newUser(false)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenTtl(accessTokenTtl)
                .sessionId(sessionId)
                .build();
    }

    private LoginResultDto handleNewUserRegistration(String mobile, String countryCode, String deviceId, DeviceInfoDto deviceInfo) {
        User user = userDomainService.createUser(RegisterSource.MOBILE, null, null, IdentityType.MOBILE);
        String userId = user.getUserId();

        UserIdentity identity = identityDomainService.bindIdentity(
                userId, IdentityType.MOBILE, mobile, countryCode, "mobile_code_login");

        identityDomainService.markVerified(userId, IdentityType.MOBILE, identity.getIdentityHash());

        userDomainService.activate(userId);

        deviceDomainService.recordDevice(userId, deviceId, deviceInfo);

        logAudit(userId, deviceId, AuditEventType.REGISTER_SUCCESS, true);

        int accessTokenTtl = 1800;
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtTokenService.generateAccessToken(
                userId, deviceId, "default", sessionId, accessTokenTtl);
        String refreshToken = refreshTokenDomainService.issueRefreshToken(
                userId, sessionId, deviceInfo.getClientId(), 7 * 24 * 60 * 60);

        return LoginResultDto.builder()
                .userId(userId)
                .newUser(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenTtl(accessTokenTtl)
                .sessionId(sessionId)
                .build();
    }

    private LoginResultDto handleNewEmailUserRegistration(String email, String clientId) {
        User user = userDomainService.createUser(RegisterSource.EMAIL, null, null, IdentityType.EMAIL);
        String userId = user.getUserId();

        UserIdentity identity = identityDomainService.bindIdentity(
                userId, IdentityType.EMAIL, email, null, "email_code_login");

        identityDomainService.markVerified(userId, IdentityType.EMAIL, identity.getIdentityHash());

        userDomainService.activate(userId);

        logAudit(userId, clientId, AuditEventType.REGISTER_SUCCESS, true);

        return LoginResultDto.builder()
                .userId(userId)
                .newUser(true)
                .sessionId(null)
                .build();
    }

    private LoginResultDto handleNewLocalMobileUserRegistration(String mobile, String clientId, DeviceInfoDto deviceInfo) {
        User user = userDomainService.createUser(RegisterSource.LOCAL_MOBILE, null, null, IdentityType.MOBILE);
        String userId = user.getUserId();

        UserIdentity identity = identityDomainService.bindIdentity(
                userId, IdentityType.MOBILE, mobile, null, "local_mobile_login");

        identityDomainService.markVerified(userId, IdentityType.MOBILE, identity.getIdentityHash());

        userDomainService.activate(userId);

        deviceDomainService.recordDevice(userId, clientId, deviceInfo);

        logAudit(userId, clientId, AuditEventType.REGISTER_SUCCESS, true);

        return LoginResultDto.builder()
                .userId(userId)
                .newUser(true)
                .sessionId(null)
                .build();
    }

    /**
     * 第三方登录统一处理逻辑。
     * <p>
     * 根据第三方主体标识（subject）查找已绑定的登录标识：
     * <ul>
     *   <li>已绑定 → 校验账号状态后直接登录</li>
     *   <li>未绑定 → 检查关联邮箱是否冲突，无冲突则自动注册新用户</li>
     *   <li>冲突 → 抛出 {@link CiamErrorCode#MERGE_REQUEST_PENDING} 异常</li>
     * </ul>
     *
     * @param userInfo   第三方返回的用户信息
     * @param type       登录标识类型
     * @param clientId   客户端标识
     * @param bindSource 绑定来源
     * @return 登录结果
     */
    private LoginResultDto handleThirdPartyLogin(ThirdPartyUserInfo userInfo,
                                                 IdentityType type,
                                                 String clientId,
                                                 String bindSource) {
        String subject = userInfo.getSubject();

        // 1. 查找第三方主体标识是否已绑定
        Optional<UserIdentity> identityOpt =
                identityDomainService.findByTypeAndValue(type, subject);

        if (identityOpt.isPresent()
                && identityOpt.get().getIdentityStatus().equals(net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityStatus.BOUND.getCode())) {
            // 已绑定 → 直接登录
            return handleExistingUserLogin(identityOpt.get(), clientId, null);
        }

        // 2. 未绑定 → 检查关联邮箱是否冲突
        String email = userInfo.getEmail();
        if (email != null && !email.isBlank()) {
            Optional<String> conflictUserId =
                    identityDomainService.checkConflict(IdentityType.EMAIL, email);
            if (conflictUserId.isPresent()) {
                // 冲突 → 进入合并流程（当前占位：抛出异常）
                logAudit(null, clientId, AuditEventType.MERGE_APPLY, false);
                throw new BusinessException(CiamErrorCode.MERGE_REQUEST_PENDING);
            }
        }

        // 3. 无冲突 → 自动注册新用户
        User user = userDomainService.createUser(RegisterSource.fromIdentityType(type), null, null, type);
        String userId = user.getUserId();

        identityDomainService.bindIdentity(userId, type, subject, null, bindSource);
        userDomainService.activate(userId);

        logAudit(userId, clientId, AuditEventType.REGISTER_SUCCESS, true);

        return LoginResultDto.builder()
                .userId(userId)
                .newUser(true)
                .sessionId(null)
                .build();
    }

    /**
     * 刷新 Token。
     *
     * @param refreshToken 用户的 Refresh Token
     * @param clientId     客户端标识
     * @return 登录结果（包含新的 access_token 和 refresh_token）
     */
    public LoginResultDto refreshToken(String refreshToken, String clientId) {
        RefreshTokenRotationResult rotationResult = refreshTokenDomainService.rotateRefreshToken(refreshToken, clientId);

        int accessTokenTtl = 1800;
        String accessToken = jwtTokenService.generateAccessToken(
                rotationResult.getUserId(), clientId, rotationResult.getScope(),
                rotationResult.getSessionId(), accessTokenTtl);

        return LoginResultDto.builder()
                .userId(rotationResult.getUserId())
                .accessToken(accessToken)
                .refreshToken(rotationResult.getNewRefreshToken())
                .accessTokenTtl(accessTokenTtl)
                .sessionId(rotationResult.getSessionId())
                .build();
    }

    private void logAudit(String userId, String deviceId, AuditEventType eventType, boolean success) {
        auditLogger.log(AuditEvent.builder()
                .userId(userId)
                .deviceId(deviceId)
                .eventType(eventType.getCategory())
                .eventName(eventType.getDescription())
                .success(success)
                .eventTime(DateTimeUtil.getNowInstant())
                .build());
    }

    /**
     * 切换设备语言。
     *
     * @param userId   用户 ID
     * @param deviceId 设备 ID
     * @param language 语言代码，如 zh-CN, en-US
     */
    public void changeLanguage(String userId, String deviceId, String language) {
        deviceDomainService.changeLanguage(userId, deviceId, language);
    }
}

