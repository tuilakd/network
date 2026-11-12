package com.example.network.security;

import com.example.network.exception.AppException;
import com.example.network.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final long LOCK_MS = 5 * 60 * 1000L;
    private static final int PURGE_THRESHOLD = 10_000;

    private static class Entry {
        int failures;
        long firstFailureAt;
        long lockedUntil;
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        Entry e = entries.get(key);
        if (e == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (e) {
            if (e.lockedUntil > now) {
                long seconds = (e.lockedUntil - now + 999) / 1000;
                throw new AppException(ErrorCode.TOO_MANY_REQUESTS,
                        "Đăng nhập sai quá nhiều lần, vui lòng thử lại sau " + seconds + " giây");
            }
        }
    }

    public void recordFailure(String key) {
        long now = System.currentTimeMillis();
        if (entries.size() > PURGE_THRESHOLD) {
            entries.values().removeIf(x -> x.lockedUntil < now && now - x.firstFailureAt > WINDOW_MS);
        }
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        synchronized (e) {
            if (e.lockedUntil != 0 && e.lockedUntil <= now) {
                e.failures = 0;
                e.lockedUntil = 0;
            }
            if (e.failures == 0 || now - e.firstFailureAt > WINDOW_MS) {
                e.failures = 0;
                e.firstFailureAt = now;
            }
            e.failures++;
            if (e.failures >= MAX_FAILURES) {
                e.lockedUntil = now + LOCK_MS;
                e.failures = 0;
            }
        }
    }

    public void recordSuccess(String key) {
        entries.remove(key);
    }
}