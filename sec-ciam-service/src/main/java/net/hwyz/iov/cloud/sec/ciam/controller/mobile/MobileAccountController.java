package net.hwyz.iov.cloud.sec.ciam.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.application.AccountBindingAppService;
import net.hwyz.iov.cloud.sec.ciam.application.AccountLifecycleAppService;
import net.hwyz.iov.cloud.sec.ciam.application.ConsentAppService;
import net.hwyz.iov.cloud.sec.ciam.application.OwnerCertificationAppService;
import net.hwyz.iov.cloud.sec.ciam.application.PasswordChangeAppService;
import net.hwyz.iov.cloud.sec.ciam.application.PasswordResetAppService;
import net.hwyz.iov.cloud.sec.ciam.application.UserProfileAppService;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto.*;
import net.hwyz.iov.cloud.sec.ciam.domain.enums.IdentityType;
import net.hwyz.iov.cloud.sec.ciam.domain.service.SessionDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.VerificationCodeType;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeviceDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOwnerCertStateDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamSessionDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserConsentDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
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

/**
 * 账号控制器 — 资料CRUD、绑定/解绑、会话/设备管理、密码变更/重置、注销、同意管理、车主认证状态。
 */
@RestController
@RequestMapping("/api/mobile/v1/account")
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
    public ApiResponse<CiamUserProfileDo> getProfile(@RequestParam String userId) {
        return ApiResponse.ok(userProfileAppService.getProfile(userId));
    }

    /** 更新用户资料 */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        userProfileAppService.updateProfile(request.getUserId(), request.getNickname(), request.getAvatarUrl(), request.getGender(), request.getBirthday(), request.getRegionCode(), request.getRegionName());
        return ApiResponse.ok();
    }

    /** 更新敏感字段 */
    @PutMapping("/profile/sensitive")
    public ApiResponse<Void> updateSensitiveField(@RequestBody @Valid UpdateSensitiveFieldRequest request) {
        userProfileAppService.updateSensitiveField(request.getUserId(), request.getField(), request.getValue(), request.getVerificationToken());
        return ApiResponse.ok();
    }

    // ---- 绑定/解绑 ----

    /** 绑定登录标识 */
    @PostMapping("/binding")
    public ApiResponse<CiamUserIdentityDo> bindIdentity(@RequestBody @Valid BindIdentityRequest request) {
        CiamUserIdentityDo result = accountBindingAppService.bindIdentity(
                request.getUserId(), IdentityType.fromValue(request.getIdentityType()), request.getIdentityValue(), request.getCountryCode(), request.getBindSource());
        return ApiResponse.ok(result);
    }

    /** 解绑登录标识 */
    @DeleteMapping("/binding")
    public ApiResponse<Void> unbindIdentity(@RequestParam String userId,
                                            @RequestParam String identityType,
                                            @RequestParam String identityHash) {
        accountBindingAppService.unbindIdentity(userId, IdentityType.fromValue(identityType), identityHash);
        return ApiResponse.ok();
    }

    // ---- 会话/设备管理 ----

    /** 查询用户活跃会话 */
    @GetMapping("/sessions")
    public ApiResponse<List<CiamSessionDo>> listSessions(@RequestParam String userId) {
        return ApiResponse.ok(sessionDomainService.findUserSessions(userId));
    }

    /** 查询用户活跃设备 */
    @GetMapping("/devices")
    public ApiResponse<List<CiamDeviceDo>> listDevices(@RequestParam String userId) {
        return ApiResponse.ok(sessionDomainService.findUserDevices(userId));
    }

    /** 下线指定会话 */
    @PostMapping("/sessions/kick")
    public ApiResponse<Void> kickSession(@RequestBody @Valid KickSessionRequest request) {
        sessionDomainService.kickSession(request.getSessionId(), request.getUserId());
        return ApiResponse.ok();
    }

    /** 下线指定设备 */
    @PostMapping("/devices/kick")
    public ApiResponse<Void> kickDevice(@RequestBody @Valid KickDeviceRequest request) {
        sessionDomainService.kickDevice(request.getDeviceId(), request.getUserId());
        return ApiResponse.ok();
    }

    // ---- 密码变更/重置 ----

    /** 修改密码 */
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        passwordChangeAppService.changePasswordAndInvalidateSessions(request.getUserId(), request.getOldPassword(), request.getNewPassword());
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
        String requestId = accountLifecycleAppService.submitDeactivationRequest(request.getUserId(), request.getRequestSource(), request.getRequestReason());
        return ApiResponse.ok(requestId);
    }

    // ---- 同意管理 ----

    /** 授予同意 */
    @PostMapping("/consent")
    public ApiResponse<CiamUserConsentDo> grantConsent(@RequestBody @Valid GrantConsentRequest request) {
        CiamUserConsentDo result = consentAppService.grantConsent(
                request.getUserId(), request.getConsentType(), request.getPolicyVersion(), request.getSourceChannel(), request.getClientType(), request.getOperateIp());
        return ApiResponse.ok(result);
    }

    /** 撤回营销同意 */
    @PostMapping("/consent/withdraw-marketing")
    public ApiResponse<Void> withdrawMarketingConsent(@RequestBody @Valid WithdrawMarketingConsentRequest request) {
        consentAppService.withdrawMarketingConsent(request.getUserId(), request.getOperateIp());
        return ApiResponse.ok();
    }

    /** 查询同意记录 */
    @GetMapping("/consent")
    public ApiResponse<List<CiamUserConsentDo>> getConsentRecords(@RequestParam String userId,
                                                                  @RequestParam(required = false) String consentType) {
        if (consentType != null && !consentType.isBlank()) {
            return ApiResponse.ok(consentAppService.getConsentByType(userId, consentType));
        }
        return ApiResponse.ok(consentAppService.getConsentRecords(userId));
    }

    /** 请求数据导出 */
    @PostMapping("/data-export")
    public ApiResponse<Void> requestDataExport(@RequestBody @Valid RequestDataExportRequest request) {
        consentAppService.requestDataExport(request.getUserId());
        return ApiResponse.ok();
    }

    /** 请求数据删除 */
    @PostMapping("/data-deletion")
    public ApiResponse<Void> requestDataDeletion(@RequestBody @Valid RequestDataDeletionRequest request) {
        consentAppService.requestDataDeletion(request.getUserId());
        return ApiResponse.ok();
    }

    // ---- 车主认证状态 ----

    /** 查询车主认证状态 */
    @GetMapping("/owner-certification")
    public ApiResponse<List<CiamOwnerCertStateDo>> getOwnerCertStatus(@RequestParam String userId) {
        return ApiResponse.ok(ownerCertificationAppService.queryCertificationStatus(userId));
    }
}
