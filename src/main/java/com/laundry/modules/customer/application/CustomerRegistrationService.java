package com.laundry.modules.customer.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.customer.application.dto.ActivateRegistrationResponse;
import com.laundry.modules.customer.application.dto.RegistrationResponse;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRegistration;
import com.laundry.modules.customer.domain.CustomerRegistrationRepository;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.CustomerSource;
import com.laundry.modules.customer.domain.CustomerStatus;
import com.laundry.modules.customer.domain.RegistrationStatus;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin kích hoạt bằng tay: Admin đã xác nhận người đăng ký là chủ số trước khi gắn vào hồ sơ có sẵn. */
@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final CustomerRegistrationRepository registrationRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend.customer-reset-password-url}")
    private String resetPasswordUrl;

    @Transactional(readOnly = true)
    public Page<RegistrationResponse> listPending(Pageable pageable) {
        return registrationRepository.findByStatusOrderByCreatedAtAscIdAsc(RegistrationStatus.PENDING, pageable)
                .map(r -> RegistrationResponse.from(r, customerRepository.findByPhone(r.getPhone()).orElse(null)));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return registrationRepository.countByStatus(RegistrationStatus.PENDING);
    }

    @Transactional
    public ActivateRegistrationResponse activate(Long id, String adminUsername) {
        CustomerRegistration registration = lockRegistration(id);
        registration.requirePending();
        String email = registration.getEmail();
        Customer customer = profileOf(registration);
        // Hồ sơ do nhân viên tạo từ trước: giữ nguyên họ tên, VIP, ghi chú, địa chỉ; chỉ gắn thông tin đăng nhập
        boolean linked = customer.getSource() != CustomerSource.SELF_REGISTERED;

        if (customer.getStatus() == CustomerStatus.INACTIVE) {
            throw new BusinessException("Hồ sơ khách " + customer.getCustomerCode()
                    + " đang ngừng hoạt động. Hãy kích hoạt lại hồ sơ hoặc từ chối yêu cầu.");
        }
        if (customer.hasAccount()) {
            throw new BusinessException("Số điện thoại đã có tài khoản");
        }
        if (email != null && customerRepository.existsByEmailIgnoreCaseAndIdNot(email, customer.getId())) {
            throw new BusinessException("Email đã được khách khác sử dụng");
        }
        customer.setPasswordHash(registration.getPasswordHash());
        customer.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        if (email != null) customer.setEmail(email);
        registration.activate(customer.getId(), adminUsername);

        if (email != null) {
            eventPublisher.publishEvent(CustomerAccountMailListener.activated(email, customer.getFullName(), loginUrl()));
        }
        auditLogService.log(new AuditEntry(AuditAction.CUSTOMER_ACCOUNT, EntityType.CUSTOMER_REGISTRATION,
                registration.getId(), null,
                Map.of("event", linked ? "ACTIVATED_LINKED" : "ACTIVATED_NEW", "customerCode", customer.getCustomerCode()),
                null));
        return new ActivateRegistrationResponse(customer.getId(), customer.getCustomerCode(), linked);
    }

    @Transactional
    public void reject(Long id, String adminUsername) {
        CustomerRegistration registration = lockRegistration(id);
        registration.reject(adminUsername);
        // Hồ sơ sinh ra từ chính yêu cầu này mà chưa có đơn thì xoá, để đăng ký rác không đọng lại trong danh sách khách
        Customer customer = registration.getCustomerId() == null ? null
                : customerRepository.findById(registration.getCustomerId()).orElse(null);
        boolean removeProfile = customer != null && customer.getSource() == CustomerSource.SELF_REGISTERED
                && !customer.hasAccount() && !customerRepository.hasOrders(customer.getId());
        if (removeProfile) {
            registration.unlinkCustomer();
            customerRepository.delete(customer);
        }
        auditLogService.log(new AuditEntry(AuditAction.CUSTOMER_ACCOUNT, EntityType.CUSTOMER_REGISTRATION,
                registration.getId(), null,
                Map.of("event", "REJECTED", "phone", registration.getPhone(), "profileRemoved", removeProfile), null));
    }

    /** Yêu cầu tạo trước khi hồ sơ được tạo lúc đăng ký thì chưa có customerId: tìm theo SĐT, không có thì tạo. */
    private Customer profileOf(CustomerRegistration registration) {
        if (registration.getCustomerId() != null) {
            return customerRepository.findById(registration.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ khách của yêu cầu"));
        }
        return customerRepository.findByPhone(registration.getPhone())
                .orElseGet(() -> customerService.createSelfRegistered(registration.getFullName(), registration.getPhone()));
    }

    private CustomerRegistration lockRegistration(Long id) {
        return registrationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu đăng ký"));
    }

    private String loginUrl() {
        URI uri = URI.create(resetPasswordUrl);
        return uri.getScheme() + "://" + uri.getAuthority() + "/login";
    }
}
