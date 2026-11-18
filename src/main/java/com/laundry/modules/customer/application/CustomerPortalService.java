package com.laundry.modules.customer.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.auth.application.dto.AuthDTO;
import com.laundry.modules.customer.application.dto.ChangeMyPasswordRequest;
import com.laundry.modules.customer.application.dto.CustomerProfileResponse;
import com.laundry.modules.customer.application.dto.UpdateMyProfileRequest;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRegistrationRepository;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.RegistrationStatus;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerPortalService {

    private final CustomerSessionService sessionService;
    private final CustomerAuthService authService;
    private final CustomerRepository customerRepository;
    private final CustomerRegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public CustomerProfileResponse getMe(Authentication authentication) {
        return CustomerProfileResponse.from(sessionService.requireCurrent(authentication));
    }

    @Transactional
    public CustomerProfileResponse updateMe(Authentication authentication, UpdateMyProfileRequest request) {
        Customer customer = sessionService.requireCurrent(authentication);
        String email = IdentifierNormalizer.normalizeEmail(request.email());
        if (email != null && !IdentifierNormalizer.isValidEmail(email)) {
            throw new BusinessException("Email không hợp lệ", "email");
        }
        if (email != null && !email.equalsIgnoreCase(Objects.toString(customer.getEmail(), ""))
                && (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, customer.getId())
                    || registrationRepository.existsByEmailIgnoreCaseAndStatus(email, RegistrationStatus.PENDING))) {
            throw new BusinessException("Email đã được sử dụng", "email");
        }
        String fullName = request.fullName().trim();
        String address = request.address() == null || request.address().isBlank() ? null : request.address().trim();

        Map<String, Object> oldData = new HashMap<>();
        Map<String, Object> newData = new HashMap<>();
        diff("fullName", customer.getFullName(), fullName, oldData, newData);
        diff("email", customer.getEmail(), email, oldData, newData);
        diff("address", customer.getAddress(), address, oldData, newData);

        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setAddress(address);
        try {
            customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Email đã được sử dụng", "email");
        }
        if (!newData.isEmpty()) {
            newData.put("event", "PROFILE_UPDATED");
            auditLogService.logAsCustomer(new AuditEntry(AuditAction.CUSTOMER_ACCOUNT, EntityType.CUSTOMER,
                    customer.getId(), oldData, newData, null), customer.getCustomerCode());
        }
        return CustomerProfileResponse.from(customer);
    }

    /** Trả cặp token mới để phiên hiện tại tiếp tục; các phiên khác bị thu hồi nhờ passwordChangedAt. */
    @Transactional
    public AuthDTO changePassword(Authentication authentication, ChangeMyPasswordRequest request) {
        Customer customer = sessionService.requireCurrent(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), customer.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng", "currentPassword");
        }
        CustomerAuthService.requireValidPassword(request.newPassword(), "newPassword");
        customer.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        customer.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        auditLogService.logAsCustomer(new AuditEntry(AuditAction.CUSTOMER_ACCOUNT, EntityType.CUSTOMER,
                customer.getId(), null, new HashMap<>(Map.of("event", "PASSWORD_CHANGED")), null), customer.getCustomerCode());
        return authService.issueTokens(customer.getId());
    }

    private static void diff(String field, Object oldValue, Object newValue, Map<String, Object> oldData, Map<String, Object> newData) {
        if (!Objects.equals(oldValue, newValue)) {
            oldData.put(field, oldValue);
            newData.put(field, newValue);
        }
    }
}
