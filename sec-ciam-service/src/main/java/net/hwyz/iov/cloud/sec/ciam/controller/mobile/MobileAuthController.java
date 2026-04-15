package net.hwyz.iov.cloud.sec.ciam.controller.mobile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.sec.ciam.application.AuthenticationAppService;
import net.hwyz.iov.cloud.sec.ciam.application.LoginResult;
import net.hwyz.iov.cloud.sec.ciam.controller.mobile.dto.*;
import net.hwyz.iov.cloud.sec.ciam.domain.service.CaptchaDomainService;
import net.hwyz.iov.cloud.sec.ciam.domain.service.VerificationCodeService;
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
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid SendMobileCodeRequest req) {
        authenticationAppService.sendMobileVerificationCode(req.getMobile(), req.getCountryCode(), clientId);
        return ApiResponse.ok();
    }

    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmailCode(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid SendEmailCodeRequest req) {
        authenticationAppService.sendEmailVerificationCode(req.getEmail(), clientId);
        return ApiResponse.ok();
    }

    @PostMapping("/login/mobile")
    public ApiResponse<LoginResult> loginByMobile(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid MobileLoginRequest req) {
        LoginResult result = authenticationAppService.loginByMobileCode(
                req.getMobile(), req.getCountryCode(), req.getCode(), clientId, req.getDeviceInfo());
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/email-password")
    public ApiResponse<LoginResult> loginByEmailPassword(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid EmailPasswordLoginRequest req) {
        LoginResult result = authenticationAppService.loginByEmailPassword(
                req.getEmail(), req.getPassword(), clientId, req.getCaptchaId(), req.getCaptchaAnswer());
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/email-code")
    public ApiResponse<LoginResult> loginByEmailCode(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid EmailCodeLoginRequest req) {
        LoginResult result = authenticationAppService.loginByEmailCode(req.getEmail(), req.getCode(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/wechat")
    public ApiResponse<LoginResult> loginByWechat(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResult result = authenticationAppService.loginByWechat(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/apple")
    public ApiResponse<LoginResult> loginByApple(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResult result = authenticationAppService.loginByApple(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/google")
    public ApiResponse<LoginResult> loginByGoogle(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid ThirdPartyLoginRequest req) {
        LoginResult result = authenticationAppService.loginByGoogle(req.getToken(), clientId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/login/local-mobile")
    public ApiResponse<LoginResult> loginByLocalMobile(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid LocalMobileLoginRequest req) {
        LoginResult result = authenticationAppService.loginByLocalMobile(req.getToken(), clientId, req.getDeviceInfo());
        return ApiResponse.ok(result);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody @Valid LogoutRequest req) {
        authenticationAppService.logout(req.getSessionId(), req.getUserId(), clientId);
        return ApiResponse.ok();
    }
}
