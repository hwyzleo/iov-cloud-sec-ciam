package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.api.vo.DeviceVO;
import net.hwyz.iov.cloud.sec.ciam.api.vo.SessionVO;
import net.hwyz.iov.cloud.sec.ciam.api.vo.UserProfileVO;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountBindingAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.AccountLifecycleAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.ConsentAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.OwnerCertificationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.PasswordChangeAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.PasswordResetAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.UserProfileAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.DeviceMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.SessionMapper;
import net.hwyz.iov.cloud.sec.ciam.service.application.mapper.UserProfileMapper;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.dto.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.SessionDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeType;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserConsentDo;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号控制器 — 资料CRUD、绑定/解绑、会话/设备管理、密码变更/重置、注销、同意管理、车主认证状态。
 */
@RestController
@RequestMapping("/api/mobile/account/v1")
@RequiredArgsConstructor
public class MobileAccountController {

    private final UserProfileAppService userProfileAppService;
    private final AccountBindingAppService accountBindingAppService;
    private final SessionDomainService sessionDomainService;
    private final PasswordChangeAppService passwordChangeAppService;
    private final PasswordResetAppService passwordResetAppService;
    private final AccountLifecycleAppService accountLifecycleAppService;
    private final ConsentAppService consentAppService;
    private final OwnerCertificationAppService ownerCertificationAppService;

    private final UserProfileMapper userProfileMapper = UserProfileMapper.INSTANCE;
    private final SessionMapper sessionMapper = SessionMapper.INSTANCE;
    private final DeviceMapper deviceMapper = DeviceMapper.INSTANCE;

    // ---- 用户资料 ----

    /** 查询用户资料 */
    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> getProfile() {
        String userId = SecurityContextHolder.getUserId();
        var profile = userProfileAppService.getProfile(userId);
        var domainModel = userProfileMapper.toDomain(profile);
        UserProfileVO vo = userProfileMapper.toVo(domainModel);
        return ApiResponse.ok(vo);
    }

    /** 更新用户资料 */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        String userId = SecurityContextHolder.getUserId();
        userProfileAppService.updateProfile(userId, request.getNickname(), request.getAvatarUrl(), request.getGender(), request.getBirthday(), request.getRegionCode(), request.getRegionName());
        return ApiResponse.ok();
    }

    /** 更新敏感字段 */
    @PutMapping("/profile/sensitive")
    public ApiResponse<Void> updateSensitiveField(@RequestBody @Valid UpdateSensitiveFieldRequest request) {
        String userId = SecurityContextHolder.getUserId();
        userProfileAppService.updateSensitiveField(userId, request.getField(), request.getValue(), request.getVerificationToken());
        return ApiResponse.ok();
    }

    // ---- 绑定/解绑 ----

    /** 绑定登录标识 */
    @PostMapping("/binding")
    public ApiResponse<CiamUserIdentityDo> bindIdentity(@RequestBody @Valid BindIdentityRequest request) {
        String userId = SecurityContextHolder.getUserId();
        CiamUserIdentityDo result = accountBindingAppService.bindIdentity(
                userId, IdentityType.fromCode(request.getIdentityType()), request.getIdentityValue(), request.getCountryCode(), request.getBindSource());
        return ApiResponse.ok(result);
    }

    /** 解绑登录标识 */
    @DeleteMapping("/binding")
    public ApiResponse<Void> unbindIdentity(@RequestParam String identityType,
                                            @RequestParam String identityHash) {
        String userId = SecurityContextHolder.getUserId();
        accountBindingAppService.unbindIdentity(userId, IdentityType.fromCode(identityType), identityHash);
        return ApiResponse.ok();
    }

    // ---- 会话/设备管理 ----

    /** 查询用户活跃会话 */
    @GetMapping("/sessions")
    public ApiResponse<List<SessionVO>> listSessions() {
        String userId = SecurityContextHolder.getUserId();
        var sessions = sessionDomainService.findUserSessions(userId);
        List<SessionVO> voList = sessions.stream()
            .map(s -> {
                var domainModel = sessionMapper.toDomain(s);
                return sessionMapper.toVo(domainModel);
            })
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }

    /** 查询用户活跃设备 */
    @GetMapping("/devices")
    public ApiResponse<List<DeviceVO>> listDevices() {
        String userId = SecurityContextHolder.getUserId();
        var devices = sessionDomainService.findUserDevices(userId);
        List<DeviceVO> voList = devices.stream()
            .map(d -> {
                var domainModel = deviceMapper.toDomain(d);
                return deviceMapper.toVo(domainModel);
            })
            .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }

    /** 下线指定会话 */
    @PostMapping("/sessions/kick")
    public ApiResponse<Void> kickSession(@RequestBody @Valid KickSessionRequest request) {
        String userId = SecurityContextHolder.getUserId();
        sessionDomainService.kickSession(request.getSessionId(), userId);
        return ApiResponse.ok();
    }

    /** 下线指定设备 */
    @PostMapping("/devices/kick")
    public ApiResponse<Void> kickDevice(@RequestBody @Valid KickDeviceRequest request) {
        String userId = SecurityContextHolder.getUserId();
        sessionDomainService.kickDevice(request.getDeviceId(), userId);
        return ApiResponse.ok();
    }

    // ---- 密码变更/重置 ----

    /** 修改密码 */
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        String userId = SecurityContextHolder.getUserId();
        passwordChangeAppService.changePasswordAndInvalidateSessions(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.ok();
    }

    /** 忘记密码 — 请求重置（手机号） */
    @PostMapping("/password/reset/request-mobile")
    public ApiResponse<String> requestResetByMobile(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid RequestResetByMobileRequest request) {
        String userId = passwordResetAppService.requestResetByMobile(request.getMobile(), request.getCountryCode(), clientId);
        return ApiResponse.ok(userId);
    }

    /** 忘记密码 — 请求重置（邮箱） */
    @PostMapping("/password/reset/request-email")
    public ApiResponse<String> requestResetByEmail(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid RequestResetByEmailRequest request) {
        String userId = passwordResetAppService.requestResetByEmail(request.getEmail(), clientId);
        return ApiResponse.ok(userId);
    }

    /** 忘记密码 — 校验验证码 */
    @PostMapping("/password/reset/verify")
    public ApiResponse<Void> verifyResetCode(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid VerifyResetCodeRequest request) {
        passwordResetAppService.verifyResetCode(request.getUserId(), clientId, VerificationCodeType.valueOf(request.getType().toUpperCase()), request.getCode());
        return ApiResponse.ok();
    }

    /** 忘记密码 — 重置密码 */
    @PostMapping("/password/reset/confirm")
    public ApiResponse<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetAppService.resetPassword(request.getUserId(), request.getNewPassword());
        return ApiResponse.ok();
    }

    // ---- 注销 ----

    /** 提交注销申请 */
    @PostMapping("/deactivation")
    public ApiResponse<String> submitDeactivation(@RequestBody @Valid SubmitDeactivationRequest request) {
        String userId = SecurityContextHolder.getUserId();
        String requestId = accountLifecycleAppService.submitDeactivationRequest(userId, request.getRequestSource(), request.getRequestReason());
        return ApiResponse.ok(requestId);
    }

    // ---- 同意管理 ----

    /** 授予同意 */
    @PostMapping("/consent")
    public ApiResponse<CiamUserConsentDo> grantConsent(@RequestBody @Valid GrantConsentRequest request) {
        String userId = SecurityContextHolder.getUserId();
        CiamUserConsentDo result = consentAppService.grantConsent(
                userId, request.getConsentType(), request.getPolicyVersion(), request.getSourceChannel(), request.getClientType(), request.getOperateIp());
        return ApiResponse.ok(result);
    }

    /** 撤回营销同意 */
    @PostMapping("/consent/withdraw-marketing")
    public ApiResponse<Void> withdrawMarketingConsent(@RequestBody @Valid WithdrawMarketingConsentRequest request) {
        String userId = SecurityContextHolder.getUserId();
        consentAppService.withdrawMarketingConsent(userId, request.getOperateIp());
        return ApiResponse.ok();
    }

    /** 查询同意记录 */
    @GetMapping("/consent")
    public ApiResponse<List<CiamUserConsentDo>> getConsentRecords(@RequestParam(required = false) String consentType) {
        String userId = SecurityContextHolder.getUserId();
        if (consentType != null && !consentType.isBlank()) {
            return ApiResponse.ok(consentAppService.getConsentByType(userId, consentType));
        }
        return ApiResponse.ok(consentAppService.getConsentRecords(userId));
    }

    /** 请求数据导出 */
    @PostMapping("/data-export")
    public ApiResponse<Void> requestDataExport(@RequestBody @Valid RequestDataExportRequest request) {
        String userId = SecurityContextHolder.getUserId();
        consentAppService.requestDataExport(userId);
        return ApiResponse.ok();
    }

    /** 请求数据删除 */
    @PostMapping("/data-deletion")
    public ApiResponse<Void> requestDataDeletion(@RequestBody @Valid RequestDataDeletionRequest request) {
        String userId = SecurityContextHolder.getUserId();
        consentAppService.requestDataDeletion(userId);
        return ApiResponse.ok();
    }

    // ---- 车主认证状态 ----

    /** 查询车主认证状态 */
    @GetMapping("/owner-certification")
    public ApiResponse<List<CiamOwnerCertStateDo>> getOwnerCertStatus() {
        String userId = SecurityContextHolder.getUserId();
        return ApiResponse.ok(ownerCertificationAppService.queryCertificationStatus(userId));
    }
}
