package net.hwyz.iov.cloud.sec.ciam.service.domain.adapter;

/**
 * 短信发送适配器接口。
 * <p>
 * 领域层仅依赖此抽象接口，具体实现由基础设施层提供。
 */
public interface SmsAdapter {

    /**
     * 发送短信验证码。
     *
     * @param mobile      手机号
     * @param countryCode 国家区号（如 +86）
     * @param code        验证码
     * @return 发送结果
     */
    AdapterResult sendVerificationCode(String mobile, String countryCode, String code);
}
