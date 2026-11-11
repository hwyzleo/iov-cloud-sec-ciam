package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.AvatarUploadRegistry;
import net.hwyz.iov.cloud.sec.ciam.service.domain.gateway.ImageObjectInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的头像上传任务归属登记（SEC-CIAM-DSN-CR-004 §4.2）。
 * <p>
 * 值编码：
 * <ul>
 *   <li>PENDING：{@code userId}</li>
 *   <li>COMPLETED：{@code userId|C|contentType|size|objectId|accessUrl}</li>
 * </ul>
 * 所有 key 均带 TTL 自动过期，不使用数据库表。
 */
@Component
@RequiredArgsConstructor
public class RedisAvatarUploadRegistry implements AvatarUploadRegistry {

    /**
     * 头像上传任务键前缀，与验证码键空间隔离
     */
    static final String KEY_PREFIX = "ciam:avatar:upload:";
    /**
     * 已完成确认状态标记
     */
    private static final String COMPLETED_MARK = "C";
    /**
     * 已完成状态字段数
     */
    private static final int COMPLETED_PARTS = 6;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void bind(String uploadId, String userId, long ttlSeconds) {
        redisTemplate.opsForValue().set(key(uploadId), userId, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> findOwner(String uploadId) {
        String value = redisTemplate.opsForValue().get(key(uploadId));
        if (StrUtil.isBlank(value)) {
            return Optional.empty();
        }
        String owner = value.split("\\|", -1)[0];
        return StrUtil.isBlank(owner) ? Optional.empty() : Optional.of(owner);
    }

    @Override
    public Optional<ImageObjectInfo> findCompletion(String uploadId) {
        String value = redisTemplate.opsForValue().get(key(uploadId));
        if (StrUtil.isBlank(value)) {
            return Optional.empty();
        }
        String[] parts = value.split("\\|", COMPLETED_PARTS);
        if (parts.length < COMPLETED_PARTS || !COMPLETED_MARK.equals(parts[1])) {
            return Optional.empty();
        }
        return Optional.of(ImageObjectInfo.builder()
                .contentType(emptyToNull(parts[2]))
                .size(parseSize(parts[3]))
                .objectId(emptyToNull(parts[4]))
                .accessUrl(parts[5])
                .build());
    }

    @Override
    public void markCompleted(String uploadId, String userId, ImageObjectInfo object, long ttlSeconds) {
        String value = String.join("|",
                userId,
                COMPLETED_MARK,
                emptyToEmpty(object == null ? null : object.getContentType()),
                object == null || object.getSize() == null ? "" : String.valueOf(object.getSize()),
                emptyToEmpty(object == null ? null : object.getObjectId()),
                emptyToEmpty(object == null ? null : object.getAccessUrl()));
        redisTemplate.opsForValue().set(key(uploadId), value, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void release(String uploadId) {
        redisTemplate.delete(key(uploadId));
    }

    private String key(String uploadId) {
        return KEY_PREFIX + uploadId;
    }

    private String emptyToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return StrUtil.isBlank(value) ? null : value;
    }

    private Long parseSize(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
