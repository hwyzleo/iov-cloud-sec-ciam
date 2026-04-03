package net.hwyz.iov.cloud.sec.ciam.domain.adapter;

/**
 * 邮件发送适配器接口。
 * <p>
 * 领域层仅依赖此抽象接口，具体实现由基础设施层提供。
 */
public interface EmailAdapter {

    /**
     * 发送邮箱验证码。
     *
     * @param email 邮箱地址
     * @param code  验证码
     * @return 发送结果
     */
    AdapterResult sendVerificationCode(String email, String code);
}
