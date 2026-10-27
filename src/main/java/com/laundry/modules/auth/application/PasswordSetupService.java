    package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.auth.application.dto.PasswordSetupRequestedEvent;
import com.laundry.modules.auth.domain.User;
import java.time.Duration;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

    @Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordSetupService {

    static String TOKEN_KEY = "laundry:auth:setup_password:token:";
    static String USER_KEY = "laundry:auth:setup_password:user:";
    static Duration TTL = Duration.ofDays(1);

    ApplicationEventPublisher eventPublisher;
    StringRedisTemplate redis;

    @NonFinal
    @Value("${app.frontend.setup-password-url}")
    String frontendUrl;

    @Transactional
    public void sendSetupMail(User user) {
        String oldToken = redis.opsForValue().get(USER_KEY + user.getId());
        if (oldToken != null) {
            redis.delete(TOKEN_KEY + oldToken);
        }

        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(TOKEN_KEY + token, String.valueOf(user.getId()), TTL);
        redis.opsForValue().set(USER_KEY + user.getId(), token, TTL);

        String link = frontendUrl + "?token=" + token;
        eventPublisher.publishEvent(
                new PasswordSetupRequestedEvent(user.getEmail(), user.getUsername(), link));
    }

    public Long consumeToken(String token) {
        String userId = redis.opsForValue().getAndDelete(TOKEN_KEY + token);
        if (userId == null) {
            throw new BusinessException("Link không hợp lệ hoặc đã hết hạn");
        }
        redis.delete(USER_KEY + userId);
        restoreOnRollback(token, userId);
        return Long.valueOf(userId);
    }

    private void restoreOnRollback(String token, String userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) return;
                // Chỉ khôi phục nếu admin chưa gửi lại link mới trong lúc đó
                Boolean restored = redis.opsForValue().setIfAbsent(USER_KEY + userId, token, TTL);
                if (Boolean.TRUE.equals(restored)) {
                    redis.opsForValue().set(TOKEN_KEY + token, userId, TTL);
                }
            }
        });
    }

}
