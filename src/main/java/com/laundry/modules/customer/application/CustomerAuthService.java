package com.laundry.modules.customer.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.security.JwtUtil;
import com.laundry.common.security.RefreshTokenStore;
import com.laundry.modules.auth.application.dto.AuthDTO;
import com.laundry.modules.customer.application.dto.CustomerLoginRequest;
import com.laundry.modules.customer.application.dto.CustomerRegisterRequest;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRegistration;
import com.laundry.modules.customer.domain.CustomerRegistrationRepository;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.CustomerStatus;
import com.laundry.modules.customer.domain.RegistrationStatus;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.setting.application.SettingService;
import com.laundry.modules.setting.domain.SettingKey;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    static final String INVALID_PASSWORD = "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số";
    private static final String WRONG_CREDENTIALS = "Email/số điện thoại hoặc mật khẩu không đúng";
    private static final String ATTEMPT_KEY = "laundry:customer-auth:attempt:";
    private static final String LOCK_KEY = "laundry:customer-auth:locked:";

    private final CustomerRepository customerRepository;
    private final CustomerRegistrationRepository registrationRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;
    private final StringRedisTemplate redis;
    private final SettingService settingService;
    private final AuditLogService auditLogService;

    // So khớp với hash giả khi không tìm thấy tài khoản để thời gian phản hồi không lộ việc tài khoản có tồn tại
    private volatile String dummyHash;

    @Transactional
    public void register(CustomerRegisterRequest request) {
        String phone = IdentifierNormalizer.normalizePhone(request.phone())
                .orElseThrow(() -> new BusinessException("Số điện thoại không hợp lệ", "phone"));
        String email = IdentifierNormalizer.normalizeEmail(request.email());
        if (email != null && !IdentifierNormalizer.isValidEmail(email)) {
            throw new BusinessException("Email không hợp lệ", "email");
        }
        requireValidPassword(request.password(), "password");

        // Hồ sơ cùng SĐT nhưng chưa có tài khoản thì không báo gì, để không lộ hồ sơ trước khi Admin xác nhận
        if (customerRepository.findByPhone(phone).filter(Customer::hasAccount).isPresent()) {
            throw new BusinessException("Số điện thoại đã có tài khoản", "phone");
        }
        if (registrationRepository.existsByPhoneAndStatus(phone, RegistrationStatus.PENDING)) {
            throw new BusinessException("Số điện thoại đang chờ tiệm kích hoạt", "phone");
        }
        if (email != null && isEmailTaken(email)) {
            throw new BusinessException("Email đã được sử dụng", "email");
        }

        String fullName = request.fullName().trim();
        // Hồ sơ có ngay lúc đăng ký để hiện trong danh sách khách; tài khoản chỉ mở khi Admin duyệt
        Customer customer = customerRepository.findByPhone(phone)
                .orElseGet(() -> customerService.createSelfRegistered(fullName, phone));

        CustomerRegistration registration;
        try {
            registration = registrationRepository.saveAndFlush(CustomerRegistration.builder()
                    .fullName(fullName)
                    .customerId(customer.getId())
                    .phone(phone)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Hai người gửi cùng lúc: index một phần trên yêu cầu PENDING chặn bản thứ hai
            boolean phoneTaken = registrationRepository.existsByPhoneAndStatus(phone, RegistrationStatus.PENDING);
            throw phoneTaken
                    ? new BusinessException("Số điện thoại đang chờ tiệm kích hoạt", "phone")
                    : new BusinessException("Email đã được sử dụng", "email");
        }

        Map<String, Object> newData = new HashMap<>();
        newData.put("event", "REGISTERED");
        newData.put("fullName", registration.getFullName());
        newData.put("phone", phone);
        newData.put("email", email);
        auditLogService.log(new AuditEntry(AuditAction.CUSTOMER_ACCOUNT, EntityType.CUSTOMER_REGISTRATION,
                registration.getId(), null, newData, null));
    }

    @Transactional
    public AuthDTO login(CustomerLoginRequest request) {
        String raw = request.identifier().trim();
        boolean byEmail = IdentifierNormalizer.isEmail(raw);
        String identifier = byEmail
                ? IdentifierNormalizer.normalizeEmail(raw)
                : IdentifierNormalizer.normalizePhone(raw).orElse(raw);

        Optional<Customer> customer = (byEmail
                ? customerRepository.findByEmailIgnoreCase(identifier)
                : customerRepository.findByPhone(identifier))
                .filter(Customer::hasAccount);
        String lockSubject = customer.map(c -> "c" + c.getId()).orElse(identifier);
        ensureNotLocked(lockSubject);

        if (customer.isPresent()) {
            Customer c = customer.get();
            if (!passwordEncoder.matches(request.password(), c.getPasswordHash())) {
                throw wrongCredentials(lockSubject);
            }
            if (c.getStatus() != CustomerStatus.ACTIVE) {
                throw new BusinessException("Tài khoản đã ngừng hoạt động, vui lòng liên hệ tiệm");
            }
            resetAttempts(lockSubject);
            c.setLastLoginAt(Instant.now());
            return issueTokens(c.getId());
        }

        Optional<CustomerRegistration> pending = byEmail
                ? registrationRepository.findByEmailIgnoreCaseAndStatus(identifier, RegistrationStatus.PENDING)
                : registrationRepository.findByPhoneAndStatus(identifier, RegistrationStatus.PENDING);
        String hash = pending.map(CustomerRegistration::getPasswordHash).orElseGet(this::dummyHash);
        if (passwordEncoder.matches(request.password(), hash) && pending.isPresent()) {
            throw new BusinessException("Tài khoản đang chờ tiệm kích hoạt");
        }
        throw wrongCredentials(lockSubject);
    }

    @Transactional(readOnly = true)
    public AuthDTO refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw CustomerSessionService.invalidSession();
        Claims claims = jwtUtil.parseCustomerRefresh(refreshToken).orElseThrow(CustomerSessionService::invalidSession);
        if (!refreshTokenStore.consume(claims)) throw CustomerSessionService.invalidSession();
        Customer customer = JwtUtil.customerIdOf(claims.getSubject())
                .flatMap(customerRepository::findById)
                .filter(c -> CustomerSessionService.isSessionValid(c, claims.getIssuedAt().toInstant()))
                .orElseThrow(CustomerSessionService::invalidSession);
        return issueTokens(customer.getId());
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        jwtUtil.parseCustomerRefresh(refreshToken).ifPresent(refreshTokenStore::consume);
    }

    AuthDTO issueTokens(Long customerId) {
        return new AuthDTO(jwtUtil.generateCustomerToken(customerId), jwtUtil.generateCustomerRefreshToken(customerId));
    }

    /** Email đã thuộc hồ sơ khác hoặc một yêu cầu đang chờ. */
    boolean isEmailTaken(String email) {
        return customerRepository.existsByEmailIgnoreCase(email)
                || registrationRepository.existsByEmailIgnoreCaseAndStatus(email, RegistrationStatus.PENDING);
    }

    static void requireValidPassword(String password, String field) {
        if (!IdentifierNormalizer.isValidPassword(password)) {
            throw new BusinessException(INVALID_PASSWORD, field);
        }
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            hash = passwordEncoder.encode(UUID.randomUUID().toString());
            dummyHash = hash;
        }
        return hash;
    }

    private void ensureNotLocked(String subject) {
        if (Boolean.TRUE.equals(redis.hasKey(LOCK_KEY + subject))) {
            int lockMinutes = settingService.getValue(SettingKey.LOCK_MINUTES, Integer.class);
            throw new BusinessException("Tài khoản tạm khoá do đăng nhập sai nhiều lần. Vui lòng thử lại sau " + lockMinutes + " phút.");
        }
    }

    private BusinessException wrongCredentials(String subject) {
        int maxAttempts = settingService.getValue(SettingKey.MAX_LOGIN_FAIL, Integer.class);
        int lockMinutes = settingService.getValue(SettingKey.LOCK_MINUTES, Integer.class);
        Long attempts = redis.opsForValue().increment(ATTEMPT_KEY + subject);
        redis.expire(ATTEMPT_KEY + subject, Duration.ofMinutes(5));
        if (attempts != null && attempts >= maxAttempts) {
            redis.opsForValue().set(LOCK_KEY + subject, "true", Duration.ofMinutes(lockMinutes));
            redis.delete(ATTEMPT_KEY + subject);
        }
        return new BusinessException(WRONG_CREDENTIALS);
    }

    private void resetAttempts(String subject) {
        redis.delete(ATTEMPT_KEY + subject);
        redis.delete(LOCK_KEY + subject);
    }
}
