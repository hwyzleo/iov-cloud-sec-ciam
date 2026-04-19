package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.store;

import net.hwyz.iov.cloud.sec.ciam.service.domain.service.VerificationCodeStore;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的验证码存储实现，用于单元测试。
 */
public class InMemoryVerificationCodeStore implements VerificationCodeStore {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public void saveCode(String key, String code, int ttlSeconds) {
        store.put(key, new Entry(code, Instant.now().plusSeconds(ttlSeconds)));
    }

    @Override
    public Optional<String> getCode(String key) {
        Entry entry = store.get(key);
        if (entry == null || Instant.now().isAfter(entry.expireAt)) {
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    @Override
    public void deleteCode(String key) {
        store.remove(key);
    }

    @Override
    public boolean setIfAbsent(String key, int ttlSeconds) {
        Entry existing = store.get(key);
        if (existing != null && Instant.now().isBefore(existing.expireAt)) {
            return false;
        }
        store.put(key, new Entry("1", Instant.now().plusSeconds(ttlSeconds)));
        return true;
    }

    @Override
    public long incrementDailyCount(String key, int midnightTtlSeconds) {
        AtomicLong counter = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        return counter.incrementAndGet();
    }

    /** 清空所有数据（测试辅助） */
    public void clear() {
        store.clear();
        counters.clear();
    }

    private record Entry(String value, Instant expireAt) {}
}
