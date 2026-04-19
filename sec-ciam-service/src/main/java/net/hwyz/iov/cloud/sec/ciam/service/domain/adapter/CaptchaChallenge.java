package net.hwyz.iov.cloud.sec.ciam.service.domain.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 验证码挑战 DTO。
 * <p>
 * 封装图形验证码或滑块验证码的挑战信息，供前端渲染使用。
 */
@Getter
@Builder
@AllArgsConstructor
public class CaptchaChallenge {

    /** 挑战唯一标识 */
    private final String challengeId;

    /** 挑战类型：IMAGE 或 SLIDER */
    private final CaptchaType challengeType;

    /** 挑战数据（base64 图片或第三方 token URL） */
    private final String challengeData;

    /**
     * 验证码挑战类型枚举。
     */
    public enum CaptchaType {
        IMAGE,
        SLIDER
    }
}
