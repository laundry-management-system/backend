package com.laundry.modules.customer.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.CustomerStatus;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class CustomerPasswordResetService {

    private static final String TOKEN_KEY = "laundry:customer-auth:reset:token:";
    private static final String CUSTOMER_KEY = "laundry:customer-auth:reset:customer:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final CustomerRepository customerRepository;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${app.frontend.customer-reset-password-url}")
    private String resetPasswordUrl;

    /** Luôn kết thúc bình thường dù email có tồn tại hay không, để phản hồi không lộ tài khoản. */
    @Transactional(readOnly = true)
    public void requestReset(String rawEmail) {
        String email = IdentifierNormalizer.normalizeEmail(rawEmail);
        if (email == null) return;
        customerRepository.findByEmailIgnoreCase(email)
                .filter(c -> c.hasAccount() && c.getStatus() == CustomerStatus.ACTIVE)
                .ifPresent(this::issueToken);
    }

    @Transactional
    public void reset(String token, String newPassword) {
        String customerId = redis.opsForValue().getAndDelete(TOKEN_KEY + token);
        if (customerId == null) {
            throw new BusinessException("Link không hợp lệ hoặc đã hết hạn");
        }
        redis.delete(CUSTOMER_KEY + customerId);
        restoreOnRollback(token, customerId);

        CustomerAuthService.requireValidPassword(newPassword, "newPassword");
        Customer customer = customerRepository.findById(Long.valueOf(customerId))
                .filter(c -> c.hasAccount() && c.getStatus() == CustomerStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Link không hợp lệ hoặc đã hết hạn"));
        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        customer.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        auditLogService.log(new AuditEntry(AuditAction.CUSTOMER_ACCOUNT, EntityType.CUSTOMER, customer.getId(),
                null, new HashMap<>(Map.of("event", "PASSWORD_RESET", "actorCustomerCode", customer.getCustomerCode())), null));
    }

    private void issueToken(Customer customer) {
        String oldToken = redis.opsForValue().get(CUSTOMER_KEY + customer.getId());
        if (oldToken != null) redis.delete(TOKEN_KEY + oldToken);
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(TOKEN_KEY + token, String.valueOf(customer.getId()), TTL);
        redis.opsForValue().set(CUSTOMER_KEY + customer.getId(), token, TTL);
        eventPublisher.publishEvent(CustomerAccountMailListener.passwordReset(
                customer.getEmail(), customer.getFullName(), resetPasswordUrl + "?token=" + token));
    }

    // Mật khẩu mới sai quy tắc thì giao dịch rollback; trả lại token để khách sửa rồi gửi lại cùng link
    private void restoreOnRollback(String token, String customerId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) return;
                Boolean restored = redis.opsForValue().setIfAbsent(CUSTOMER_KEY + customerId, token, TTL);
                if (Boolean.TRUE.equals(restored)) {
                    redis.opsForValue().set(TOKEN_KEY + token, customerId, TTL);
                }
            }
        });
    }
}
