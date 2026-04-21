package net.hwyz.iov.cloud.sec.ciam.service.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.constant.CustomHeaders;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.sec.ciam.service.application.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.LoginResultDTO;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.DeviceInfoDTO;
import net.hwyz.iov.cloud.sec.ciam.service.controller.mobile.vo.*;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.CaptchaDomainService;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeService;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 注册、登录、登出、验证码。
 */
@RestController
@RequestMapping("/api/mobile/auth/v1")
@RequiredArgsConstructor
public class MobileAuthController {

    private final AuthenticationAppService authenticationAppService;
    private final VerificationCodeService verificationCodeService;
    private final CaptchaDomainService captchaDomainService;


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
    public ApiResponse<LoginResultDTO> loginByMobile(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestHeader(CustomHeaders.CLIENT_TYPE) String clientType,
            @RequestHeader(CustomHeaders.DEVICE_ID) String deviceId,
            @RequestHeader(CustomHeaders.PLATFORM) String platform,
            @RequestHeader(CustomHeaders.APP_VERSION) String appVersion,
            @RequestBody @Valid MobileLoginRequest req) {
        DeviceInfoDTO deviceInfo = req.getDeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setClientId(clientId);
        deviceInfo.setClientType(clientType);
        deviceInfo.setDeviceOs(platform);
        deviceInfo.setAppVersion(appVersion);
        LoginResultDTO result = authenticationAppService.loginByMobileCode(
                req.getMobile(), req.getCountryCode(), req.getCode(), deviceId, deviceInfo);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/email-password")
    public ApiResponse<LoginResultDTO> loginByEmailPassword(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid EmailPasswordLoginRequest req) {
        LoginResultDTO result = authenticationAppService.loginByEmailPassword(
                req.getEmail(), req.getPassword(), clientId, req.getCaptchaId(), req.getCaptchaAnswer());
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/email-code")
    public ApiResponse<LoginResultDTO> loginByEmailCode(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid EmailCodeLoginRequest req) {
        LoginResultDTO result = authenticationAppService.loginByEmailCode(req.getEmail(), req.getCode(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/wechat")
    public ApiResponse<LoginResultDTO> loginByWechat(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResultDTO result = authenticationAppService.loginByWechat(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/apple")
    public ApiResponse<LoginResultDTO> loginByApple(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResultDTO result = authenticationAppService.loginByApple(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/google")
    public ApiResponse<LoginResultDTO> loginByGoogle(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResultDTO result = authenticationAppService.loginByGoogle(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/local-mobile")
    public ApiResponse<LoginResultDTO> loginByLocalMobile(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid LocalMobileLoginRequest req) {
        LoginResultDTO result = authenticationAppService.loginByLocalMobile(req.getToken(), clientId, req.getDeviceInfo());
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid LogoutRequest req) {
        authenticationAppService.logout(req.getSessionId(), req.getUserId(), clientId);
        return ApiResponse.ok();
    }

    @PostMapping("/token/refresh")
    public ApiResponse<LoginResultDTO> refreshToken(
            @RequestHeader(CustomHeaders.CLIENT_ID) String clientId,
            @RequestBody @Valid RefreshTokenRequest req) {
        LoginResultDTO result = authenticationAppService.refreshToken(req.getRefreshToken(), clientId);
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
