package net.hwyz.iov.cloud.sec.ciam.service.application.assembler;

import net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo.LoginResultResponse;
import net.hwyz.iov.cloud.sec.ciam.service.application.dto.LoginResultDto2;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.CaptchaChallenge;
import org.springframework.stereotype.Component;

@Component
public class LoginResultAssembler {

    public LoginResultResponse toVoWithCaptcha(LoginResultDto2 dto) {
        if (dto == null) {
            return null;
        }
        LoginResultResponse.LoginResultResponseBuilder builder = LoginResultResponse.builder()
                .userId(dto.getUserId())
                .newUser(dto.isNewUser())
                .accessToken(dto.getAccessToken())
                .refreshToken(dto.getRefreshToken())
                .accessTokenTtl(dto.getAccessTokenTtl())
                .sessionId(dto.getSessionId())
                .challengeRequired(dto.isChallengeRequired())
                .fallbackRequired(dto.isFallbackRequired());

        CaptchaChallenge captcha = dto.getCaptchaChallenge();
        if (captcha != null) {
            builder.captchaChallenge(LoginResultResponse.CaptchaChallengeResponse.builder()
                    .challengeId(captcha.getChallengeId())
                    .challengeType(captcha.getChallengeType().name())
                    .challengeData(captcha.getChallengeData())
                    .build());
        }
        return builder.build();
    }
}