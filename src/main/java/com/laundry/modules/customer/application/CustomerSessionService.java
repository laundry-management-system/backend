package com.laundry.modules.customer.application;

import com.laundry.common.security.JwtFilter;
import com.laundry.common.security.JwtUtil;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.CustomerStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Access token không lưu trạng thái, nên mỗi request của khách kiểm lại hồ sơ:
 * ngừng hoạt động hoặc đổi mật khẩu sau lúc cấp token thì phiên bị từ chối ngay.
 */
@Service
@RequiredArgsConstructor
public class CustomerSessionService {

    private final CustomerRepository customerRepository;

    public Customer requireCurrent(Authentication authentication) {
        Long customerId = JwtUtil.customerIdOf(authentication == null ? null : authentication.getName())
                .orElseThrow(CustomerSessionService::invalidSession);
        Object issuedAt = RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtFilter.ISSUED_AT_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        Customer customer = customerRepository.findById(customerId).orElseThrow(CustomerSessionService::invalidSession);
        if (!(issuedAt instanceof Instant iat) || !isSessionValid(customer, iat)) {
            throw invalidSession();
        }
        return customer;
    }

    /** So ở mức giây vì iat của JWT chỉ có giây. */
    // shortcut: token cấp trong đúng giây đổi mật khẩu vẫn hợp lệ (cần thế để phiên vừa đổi mật khẩu dùng tiếp); chuyển sang claim phiên bản phiên nếu cần thu hồi tuyệt đối

    public static boolean isSessionValid(Customer customer, Instant issuedAt) {
        if (customer.getStatus() != CustomerStatus.ACTIVE || !customer.hasAccount()) return false;
        Instant changedAt = customer.getPasswordChangedAt();
        return changedAt == null || issuedAt.getEpochSecond() >= changedAt.getEpochSecond();
    }

    static BadCredentialsException invalidSession() {
        return new BadCredentialsException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
    }
}
