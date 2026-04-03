package net.hwyz.iov.cloud.sec.ciam.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 全局唯一用户 ID 生成器。
 * <p>
 * 生成格式为去掉连字符的 UUID v4 字符串（32 位十六进制），满足以下要求：
 * <ul>
 *   <li>平台全局唯一，不可复用</li>
 *   <li>不暴露手机号、邮箱等敏感信息</li>
 *   <li>可直接作为 OIDC {@code sub} 声明来源</li>
 *   <li>与数据库自增物理主键 {@code id} 分离</li>
 * </ul>
 */
public final class UserIdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private UserIdGenerator() {
    }

    /**
     * 生成全局唯一用户 ID。
     *
     * @return 32 位十六进制字符串（UUID v4 去连字符）
     */
    public static String generate() {
        // 使用 SecureRandom 支撑的 UUID，保证密码学安全随机性
        byte[] randomBytes = new byte[16];
        SECURE_RANDOM.nextBytes(randomBytes);
        // 设置 UUID v4 版本位
        randomBytes[6] = (byte) ((randomBytes[6] & 0x0f) | 0x40);
        // 设置 variant 位
        randomBytes[8] = (byte) ((randomBytes[8] & 0x3f) | 0x80);
        UUID uuid = uuidFromBytes(randomBytes);
        return uuid.toString().replace("-", "");
    }

    private static UUID uuidFromBytes(byte[] data) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (data[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (data[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }
}
