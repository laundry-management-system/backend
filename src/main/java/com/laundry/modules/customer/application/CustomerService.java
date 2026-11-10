package com.laundry.modules.customer.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.customer.application.dto.CreateCustomerRequest;
import com.laundry.modules.customer.application.dto.CustomerResponse;
import com.laundry.modules.customer.application.dto.UpdateCustomerRequest;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.CustomerStatus;
import com.laundry.modules.customer.infrastructure.CustomerOrderStats;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerOrderStats orderStats;
    private final AuditLogService auditLogService;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long findOrCreateForIntake(String phone, String fullName, String staffUsername) {
        return customerRepository.findByPhone(phone.trim())
                .map(customer -> {
                    if (customer.getStatus() == CustomerStatus.INACTIVE) {
                        throw new BusinessException("Hồ sơ khách đang ngừng hoạt động", "customerPhone");
                    }
                    return customer.getId();
                })
                .orElseGet(() -> createCustomer(new CreateCustomerRequest(fullName, phone, null, null), staffUsername).id());
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request, String staffUsername) {
        String phone = request.phone().trim();
        if (customerRepository.existsByPhone(phone)) {
            throw new BusinessException("Phone number '" + phone + "' is already registered in the system (BR-B01)");
        }

        Customer customer = Customer.builder()
                .customerCode("TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .fullName(request.fullName().trim())
                .phone(phone)
                .address(request.address() != null ? request.address().trim() : null)
                .note(request.note() != null ? request.note().trim() : null)
                .isVip(false)
                .status(CustomerStatus.ACTIVE)
                .createdBy(staffUsername)
                .build();

        try{
            customer = this.customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException e) {
            throw new  BusinessException("Phone number '" + phone + "' is already registered");
        }
        customer.setCustomerCode(String.format("KH%06d", customer.getId()));
        return CustomerResponse.from(customer, CustomerOrderStats.Stats.EMPTY);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByPhone(String phone) {
        String cleanPhone = phone != null ? phone.trim() : "";
        return customerRepository.findByPhone(cleanPhone)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Customer not found with phone number: " + cleanPhone));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return customerRepository.findByCustomerCode(cleanCode)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Customer not found with code: " + cleanCode));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(String search, CustomerStatus status, Pageable pageable) {
        Page<Customer> page = customerRepository.searchCustomers(search != null ? search.trim() : "", status, pageable);
        Map<Long, CustomerOrderStats.Stats> stats = orderStats.forCustomers(
                page.getContent().stream().map(Customer::getId).toList());
        return page.map(c -> CustomerResponse.from(c, stats.get(c.getId())));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findById(id);

        if (request.phone() != null && !request.phone().isBlank()) {
            String newPhone = request.phone().trim();
            String oldPhone = customer.getPhone();
            if (!newPhone.equals(oldPhone)) {
                if (customerRepository.existsByPhone(newPhone)) {
                    throw new BusinessException("Phone number '" + newPhone + "' is already registered to another customer (BR-B01)");
                }
                customer.setPhone(newPhone);
                audit(AuditAction.CUSTOMER_PHONE_CHANGE, customer, Map.of("phone", oldPhone), Map.of("phone", newPhone));
            }
        }

        customer.setFullName(request.fullName());
        customer.setAddress(request.address());
        customer.setNote(request.note());

        return toResponse(customerRepository.save(customer));
    }

    /** BR-B03: không ngừng hoạt động khách còn đơn chưa đóng. */
    @Transactional
    public CustomerResponse changeStatus(Long id, CustomerStatus status) {
        Customer customer = findById(id);
        CustomerStatus before = customer.getStatus();

        if (status == CustomerStatus.INACTIVE) {
            long openOrders = orderStats.openOrderCount(customer.getId());
            if (openOrders > 0) {
                throw new BusinessException("Khách còn " + openOrders
                        + " đơn chưa giao hoặc chưa hủy, không thể ngừng hoạt động (BR-B03)", "status");
            }
            customer.deactivate();
        } else if (status == CustomerStatus.ACTIVE) {
            customer.activate();
        }

        Customer saved = customerRepository.save(customer);
        audit(AuditAction.CUSTOMER_STATUS, saved, Map.of("status", before.name()), Map.of("status", saved.getStatus().name()));
        return toResponse(saved);
    }

    /** Bật / tắt khách VIP (UC20, chỉ Admin). */
    @Transactional
    public CustomerResponse changeVip(Long id, boolean vip) {
        Customer customer = findById(id);
        boolean before = Boolean.TRUE.equals(customer.getIsVip());
        if (before == vip) {
            throw new BusinessException(vip ? "Khách đã là VIP" : "Khách không phải VIP", "vip");
        }
        customer.setVipStatus(vip);
        Customer saved = customerRepository.save(customer);
        audit(AuditAction.CUSTOMER_VIP, saved, Map.of("isVip", before), Map.of("isVip", vip));
        return toResponse(saved);
    }

    private Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + id));
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.from(customer, orderStats.forCustomers(List.of(customer.getId())).get(customer.getId()));
    }

    private void audit(AuditAction action, Customer customer, Map<String, Object> before, Map<String, Object> after) {
        auditLogService.log(new AuditEntry(action, EntityType.CUSTOMER, customer.getId(), before, after, null,
                customer.getCustomerCode()));
    }
}
