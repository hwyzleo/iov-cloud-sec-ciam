package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AccountBindingAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AccountLifecycleAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.ConsentAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.OwnerCertificationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.PasswordChangeAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.PasswordResetAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.UserProfileAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.assembler.*;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.SessionDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeType;
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

    // ---- 用户资料 ----

    /** 查询用户资料 */
    @GetMapping("/profile")
    public ApiResponse<UserProfileVo> getProfile() {
        String userId = SecurityContextHolder.getUserId();
        return ApiResponse.ok(UserProfileAssembler.INSTANCE.toVo(userProfileAppService.getProfile(userId)));
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
    public ApiResponse<UserIdentityVo> bindIdentity(@RequestBody @Valid BindIdentityRequest request) {
        String userId = SecurityContextHolder.getUserId();
        UserIdentityVo vo = UserIdentityAssembler.INSTANCE.toVo(accountBindingAppService.bindIdentity(
                userId, IdentityType.fromCode(request.getIdentityType()), request.getIdentityValue(), request.getCountryCode(), request.getBindSource()));
        return ApiResponse.ok(vo);
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
    public ApiResponse<List<SessionVo>> listSessions() {
        String userId = SecurityContextHolder.getUserId();
        List<SessionVo> voList = sessionDomainService.findUserSessions(userId).stream()
                .map(doObj -> SessionAssembler.INSTANCE.toVo(SessionAssembler.INSTANCE.toDto(SessionAssembler.INSTANCE.toDomain(doObj))))
                .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }

    /** 查询用户活跃设备 */
    @GetMapping("/devices")
    public ApiResponse<List<DeviceVo>> listDevices() {
        String userId = SecurityContextHolder.getUserId();
        List<DeviceVo> voList = sessionDomainService.findUserDevices(userId).stream()
                .map(domain -> DeviceAssembler.INSTANCE.toVo(DeviceAssembler.INSTANCE.toDto(domain)))
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
    public ApiResponse<UserConsentVo> grantConsent(@RequestBody @Valid GrantConsentRequest request) {
        String userId = SecurityContextHolder.getUserId();
        UserConsentVo vo = UserConsentAssembler.INSTANCE.toVo(consentAppService.grantConsent(
                userId, request.getConsentType(), request.getPolicyVersion(), request.getSourceChannel(), request.getClientType(), request.getOperateIp()));
        return ApiResponse.ok(vo);
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
    public ApiResponse<List<UserConsentVo>> getConsentRecords(@RequestParam(required = false) String consentType) {
        String userId = SecurityContextHolder.getUserId();
        List<UserConsentVo> voList;
        if (consentType != null && !consentType.isBlank()) {
            voList = consentAppService.getConsentByType(userId, consentType).stream()
                    .map(UserConsentAssembler.INSTANCE::toVo)
                    .collect(Collectors.toList());
        } else {
            voList = consentAppService.getConsentRecords(userId).stream()
                    .map(UserConsentAssembler.INSTANCE::toVo)
                    .collect(Collectors.toList());
        }
        return ApiResponse.ok(voList);
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
    public ApiResponse<List<OwnerCertificationVo>> getOwnerCertStatus() {
        String userId = SecurityContextHolder.getUserId();
        List<OwnerCertificationVo> voList = ownerCertificationAppService.queryCertificationStatus(userId).stream()
                .map(OwnerCertificationAssembler.INSTANCE::toVo)
                .collect(Collectors.toList());
        return ApiResponse.ok(voList);
    }
}
