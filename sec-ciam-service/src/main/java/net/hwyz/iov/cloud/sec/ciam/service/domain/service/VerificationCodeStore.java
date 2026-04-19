package net.hwyz.iov.cloud.sec.ciam.service.domain.service;

import java.util.Optional;

/**
 * 验证码存储抽象接口。
 * <p>
 * 领域层依赖此接口，基础设施层提供 Redis 实现，测试使用内存实现。
 */
public interface VerificationCodeStore {

    /**
     * 存储验证码。
     *
     * @param key          存储键（如 sms:userId:clientId）
     * @param code         验证码
     * @param ttlSeconds   有效期（秒）
     */
    void saveCode(String key, String code, int ttlSeconds);

    /**
     * 获取验证码。
     *
     * @param key 存储键
     * @return 验证码（如存在且未过期）
     */
    Optional<String> getCode(String key);

    /**
     * 删除验证码。
     *
     * @param key 存储键
     */
    void deleteCode(String key);

    /**
     * 尝试设置频控标记（如果不存在则设置成功）。
     * <p>
     * 用于实现"1 分钟 1 次"限制：若 key 已存在则返回 false，否则设置并返回 true。
     *
     * @param key        频控键
     * @param ttlSeconds 有效期（秒）
     * @return true 表示设置成功（未被限流），false 表示已存在（被限流）
     */
    boolean setIfAbsent(String key, int ttlSeconds);

    /**
     * 递增每日计数器并返回递增后的值。
     * <p>
     * 用于实现"单日 30 次"限制。首次调用时自动创建并设置过期时间为当日午夜。
     *
     * @param key              计数键
     * @param midnightTtlSeconds 距离午夜的剩余秒数
     * @return 递增后的计数值
     */
    long incrementDailyCount(String key, int midnightTtlSeconds);
}
