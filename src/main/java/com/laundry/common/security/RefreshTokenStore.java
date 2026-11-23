package com.laundry.common.security;

import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Refresh token chỉ dùng được một lần; dùng chung cho phiên nhân viên và phiên khách (jti là UUID nên không đụng nhau). */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String BLACKLIST_KEY_PREFIX = "laundry:auth:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    /** Đánh dấu refresh token đã dùng. Trả true nếu đây là lần dùng đầu tiên. */
    public boolean consume(Claims claims) {
        Duration remaining = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        if (remaining.toMillis() <= 0) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(BLACKLIST_KEY_PREFIX + claims.getId(), "1", remaining));
    }
}
