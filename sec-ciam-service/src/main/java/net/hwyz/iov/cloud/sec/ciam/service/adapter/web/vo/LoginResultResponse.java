package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultResponse {

    private String userId;

    private boolean newUser;

    private String accessToken;

    private String refreshToken;

    private Integer accessTokenTtl;

    private String sessionId;

    private boolean challengeRequired;

    private CaptchaChallengeResponse captchaChallenge;

    private boolean fallbackRequired;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptchaChallengeResponse {
        private String challengeId;

        private String challengeData;

        private String challengeType;
    }
}