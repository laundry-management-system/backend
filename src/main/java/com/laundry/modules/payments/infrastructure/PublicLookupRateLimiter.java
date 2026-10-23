package com.laundry.modules.payments.infrastructure;

import com.laundry.common.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PublicLookupRateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;

    public PublicLookupRateLimiter(
            @Value("${app.public-lookup.max-attempts:10}") int maxAttempts,
            @Value("${app.public-lookup.window-seconds:60}") long windowSeconds) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.windowMillis = Duration.ofSeconds(Math.max(1, windowSeconds)).toMillis();
    }

    public void check(String clientAddress) {
        long now = System.currentTimeMillis();
        String key = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        AtomicBoolean allowed = new AtomicBoolean(true);
        windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= windowMillis) {
                return new Window(now, 1);
            }
            if (current.attempts >= maxAttempts) {
                allowed.set(false);
                return current;
            }
            return new Window(current.startedAt, current.attempts + 1);
        });

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis);
        }
        if (!allowed.get()) {
            throw new TooManyRequestsException("Đã vượt quá số lần tra cứu cho phép. Vui lòng thử lại sau.");
        }
    }

    private record Window(long startedAt, int attempts) {
    }
}
