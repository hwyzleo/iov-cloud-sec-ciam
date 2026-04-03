package net.hwyz.iov.cloud.sec.ciam.domain.adapter;

/**
 * 图形验证码 / 滑块验证码适配器接口。
 * <p>
 * 领域层仅依赖此抽象接口，具体实现由基础设施层提供。
 * 首发版本使用桩实现（数学运算验证码），后续可替换为真实验证码服务商。
 */
public interface CaptchaAdapter {

    /**
     * 生成验证码挑战。
     *
     * @param sessionId 会话标识，用于关联挑战与用户会话
     * @return 验证码挑战信息
     */
    CaptchaChallenge generateChallenge(String sessionId);

    /**
     * 校验验证码答案。
     *
     * @param challengeId 挑战唯一标识
     * @param answer      用户提交的答案
     * @return true 表示校验通过，false 表示校验失败
     */
    boolean verifyChallenge(String challengeId, String answer);
}
