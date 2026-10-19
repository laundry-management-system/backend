package com.laundry.modules.customers.application;

import com.laundry.common.exception.NotFoundException;
import com.laundry.common.exception.ResourceConflictException;
import com.laundry.common.exception.BusinessException;
import com.laundry.common.security.ActorIdResolver;
import com.laundry.modules.customers.application.dto.CreateCustomerRequest;
import com.laundry.modules.customers.application.dto.CustomerResponse;
import com.laundry.modules.customers.application.dto.CustomerStatusRequest;
import com.laundry.modules.customers.application.dto.UpdateCustomerRequest;
import com.laundry.modules.customers.domain.Customer;
import com.laundry.modules.customers.domain.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ActorIdResolver actorIdResolver;

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request, String username) {
        if (customerRepository.existsByPhone(request.phone())) {
            throw new ResourceConflictException("Số điện thoại đã được sử dụng");
        }
        Customer customer = Customer.create(request.fullName(), request.phone(), request.address(), request.note(),
                actorIdResolver.resolveId(username));
        try {
            return CustomerResponse.from(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Số điện thoại hoặc mã khách hàng đã tồn tại");
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(long id) {
        return customerRepository.findById(id).map(CustomerResponse::from)
                .orElseThrow(() -> NotFoundException.of("Khách hàng", id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getByPhone(String phone) {
        return customerRepository.findByPhone(phone).map(CustomerResponse::from)
                .orElseThrow(() -> NotFoundException.of("Khách hàng với số điện thoại", phone));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException("Cần truyền phone hoặc keyword để tìm khách hàng");
        }
        String value = keyword.trim();
        return customerRepository.findByFullNameContainingIgnoreCaseOrPhoneContaining(value, value).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional
    public CustomerResponse updateProfile(long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Khách hàng", id));
        customer.updateProfile(request.fullName(), request.address(), request.note());
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse changeStatus(long id, CustomerStatusRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Khách hàng", id));
        customer.changeStatus(request.status());
        return CustomerResponse.from(customer);
    }
}
