package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.constant.CustomHeaders;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.mobile.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.LoginResultDto;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.cmd.LoginByMobileCodeCmd;
import net.hwyz.iov.cloud.sec.ciam.service.application.service.AuthenticationAppService;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 注册、登录、登出、验证码。
 */
@RestController
@RequestMapping("/api/mobile/auth/v1")
@RequiredArgsConstructor
public class MobileAuthController {

    private final AuthenticationAppService authenticationAppService;


    @PostMapping("/sms/send")
    public ApiResponse<Void> sendMobileCode(
            @RequestHeader(CustomHeaders.DEVICE_ID) String deviceId,
            @RequestBody @Valid SendMobileCodeRequest req) {
        authenticationAppService.sendMobileVerificationCode(req.getMobile(), req.getCountryCode(), deviceId);
        return ApiResponse.ok();
    }

    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmailCode(
            @RequestHeader(CustomHeaders.DEVICE_ID) String deviceId,
            @RequestBody @Valid SendEmailCodeRequest req) {
        authenticationAppService.sendEmailVerificationCode(req.getEmail(), deviceId);
        return ApiResponse.ok();
    }

    @PostMapping("/login/mobile")
    public ApiResponse<LoginResultDto> loginByMobile(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestHeader(CustomHeaders.CLIENT_TYPE) String clientType,
            @RequestHeader(CustomHeaders.DEVICE_ID) String deviceId,
            @RequestHeader(CustomHeaders.PLATFORM) String platform,
            @RequestHeader(CustomHeaders.APP_VERSION) String appVersion,
            @RequestBody @Valid MobileLoginRequest req) {
        DeviceInfoDto deviceInfo = req.getDeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setClientId(clientId);
        deviceInfo.setClientType(clientType);
        deviceInfo.setDeviceOs(platform);
        deviceInfo.setAppVersion(appVersion);
        LoginResultDto result = authenticationAppService.loginByMobileCode(LoginByMobileCodeCmd.builder()
                .mobile(req.getMobile())
                .countryCode(req.getCountryCode())
                .code(req.getCode())
                .deviceId(deviceId)
                .deviceInfo(deviceInfo)
                .build());
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/email-password")
    public ApiResponse<LoginResultDto> loginByEmailPassword(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid EmailPasswordLoginRequest req) {
        LoginResultDto result = authenticationAppService.loginByEmailPassword(
                req.getEmail(), req.getPassword(), clientId, req.getCaptchaId(), req.getCaptchaAnswer());
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/email-code")
    public ApiResponse<LoginResultDto> loginByEmailCode(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid EmailCodeLoginRequest req) {
        LoginResultDto result = authenticationAppService.loginByEmailCode(req.getEmail(), req.getCode(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/wechat")
    public ApiResponse<LoginResultDto> loginByWechat(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResultDto result = authenticationAppService.loginByWechat(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/apple")
    public ApiResponse<LoginResultDto> loginByApple(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResultDto result = authenticationAppService.loginByApple(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/google")
    public ApiResponse<LoginResultDto> loginByGoogle(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResultDto result = authenticationAppService.loginByGoogle(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/local-mobile")
    public ApiResponse<LoginResultDto> loginByLocalMobile(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid LocalMobileLoginRequest req) {
        LoginResultDto result = authenticationAppService.loginByLocalMobile(req.getToken(), clientId, req.getDeviceInfo());
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid LogoutRequest req) {
        authenticationAppService.logout(req.getSessionId(), req.getUserId(), clientId);
        return ApiResponse.ok();
    }

    @PostMapping("/token/refresh")
    public ApiResponse<LoginResultDto> refreshToken(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid RefreshTokenRequest req) {
        LoginResultDto result = authenticationAppService.refreshToken(req.getRefreshToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/device/language")
    public ApiResponse<Void> changeLanguage(
            @RequestHeader(CustomHeaders.DEVICE_ID) String deviceId,
            @RequestBody @Valid ChangeLanguageRequest req) {
        String userId = SecurityContextHolder.getUserId();
        authenticationAppService.changeLanguage(userId, deviceId, req.getLanguage());
        return ApiResponse.ok();
    }
}
