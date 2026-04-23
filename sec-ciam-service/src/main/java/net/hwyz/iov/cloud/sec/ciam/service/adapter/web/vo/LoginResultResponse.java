package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultResponse {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("new_user")
    private boolean newUser;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("access_token_ttl")
    private Integer accessTokenTtl;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("challenge_required")
    private boolean challengeRequired;

    @JsonProperty("captcha_challenge")
    private CaptchaChallengeResponse captchaChallenge;

    @JsonProperty("fallback_required")
    private boolean fallbackRequired;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaptchaChallengeResponse {
        @JsonProperty("challenge_id")
        private String challengeId;

        @JsonProperty("challenge_data")
        private String challengeData;

        @JsonProperty("challenge_type")
        private String challengeType;
    }
}