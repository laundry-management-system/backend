package com.laundry.modules.customer.application;

import com.laundry.common.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Cửa sổ cố định trên Redis nên vẫn đúng khi chạy nhiều instance. */
@Component
@RequiredArgsConstructor
public class CustomerAuthRateLimiter {

    private static final String KEY_PREFIX = "laundry:customer-auth:rl:";

    private final StringRedisTemplate redis;

    public void check(String bucket, String key, int max, Duration window) {
        String redisKey = KEY_PREFIX + bucket + ":" + (key == null || key.isBlank() ? "unknown" : key);
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redis.expire(redisKey, window);
        }
        if (count != null && count > max) {
            throw new TooManyRequestsException("Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.");
        }
    }

    // shortcut: tin giá trị đầu của X-Forwarded-For vì Vercel ghi đè header này; gọi thẳng BE thì giả được, nâng cấp bằng proxy tin cậy khi BE không còn đứng sau Vercel
    public static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
