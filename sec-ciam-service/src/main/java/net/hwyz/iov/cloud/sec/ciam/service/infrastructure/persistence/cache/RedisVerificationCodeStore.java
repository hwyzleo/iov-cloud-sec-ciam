package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.cache;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的验证码存储实现。
 * <p>
 * 使用 {@link StringRedisTemplate} 操作 Redis，所有 key 均带 TTL 自动过期。
 */
@Component
@RequiredArgsConstructor
public class RedisVerificationCodeStore implements VerificationCodeStore {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveCode(String key, String code, int ttlSeconds) {
        redisTemplate.opsForValue().set(key, code, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> getCode(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void deleteCode(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean setIfAbsent(String key, int ttlSeconds) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public long incrementDailyCount(String key, int midnightTtlSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // 首次创建时设置过期时间为午夜
            redisTemplate.expire(key, midnightTtlSeconds, TimeUnit.SECONDS);
        }
        return count == null ? 0 : count;
    }
}
