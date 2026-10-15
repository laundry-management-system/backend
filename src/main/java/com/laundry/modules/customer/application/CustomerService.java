package com.laundry.modules.customer.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.customer.application.dto.CreateCustomerRequest;
import com.laundry.modules.customer.application.dto.CustomerResponse;
import com.laundry.modules.customer.application.dto.UpdateCustomerRequest;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRepository;
import com.laundry.modules.customer.domain.CustomerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request, String staffUsername) {
        String phone = request.phone().trim();
        if (customerRepository.existsByPhone(phone)) {
            throw new BusinessException("Số điện thoại '" + phone + "' đã được đăng ký trong hệ thống (BR-B01)");
        }

        String customerCode = generateCustomerCode();

        Customer customer = Customer.builder()
                .customerCode(customerCode)
                .fullName(request.fullName().trim())
                .phone(phone)
                .address(request.address() != null ? request.address().trim() : null)
                .note(request.note() != null ? request.note().trim() : null)
                .isVip(false)
                .status(CustomerStatus.ACTIVE)
                .createdBy(staffUsername != null ? staffUsername : "SYSTEM")
                .build();

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin khách hàng với ID: " + id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByPhone(String phone) {
        String cleanPhone = phone != null ? phone.trim() : "";
        return customerRepository.findByPhone(cleanPhone)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng với số điện thoại: " + cleanPhone));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return customerRepository.findByCustomerCode(cleanCode)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng với mã: " + cleanCode));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(String search, Pageable pageable) {
        return customerRepository.searchCustomers(search != null ? search.trim() : "", pageable)
                .map(CustomerResponse::from);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request, String staffUsername) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng với ID: " + id));

        if (request.phone() != null && !request.phone().isBlank()) {
            String newPhone = request.phone().trim();
            if (customerRepository.existsByPhoneAndIdNot(newPhone, id)) {
                throw new BusinessException("Số điện thoại '" + newPhone + "' đã thuộc về một khách hàng khác (BR-B01)");
            }
            customer.updatePhone(newPhone);
        }

        customer.updateInfo(request.fullName(), request.address(), request.note());
        Customer saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }

    @Transactional
    public CustomerResponse changeStatus(Long id, CustomerStatus status) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng với ID: " + id));

        if (status == CustomerStatus.INACTIVE) {
            customer.deactivate();
        } else if (status == CustomerStatus.ACTIVE) {
            customer.activate();
        }

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }

    private String generateCustomerCode() {
        long nextId = customerRepository.findTopByOrderByIdDesc()
                .map(c -> c.getId() + 1)
                .orElse(1L);

        String candidateCode = String.format("KH%06d", nextId);
        long counter = nextId;
        while (customerRepository.existsByPhone(candidateCode) || customerRepository.findByCustomerCode(candidateCode).isPresent()) {
            counter++;
            candidateCode = String.format("KH%06d", counter);
        }
        return candidateCode;
    }
}
