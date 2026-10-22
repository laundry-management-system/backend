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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

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
        return CustomerResponse.from(customer);
    }

    public CustomerResponse getCustomerById(Long id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + id));
    }

    public CustomerResponse getCustomerByPhone(String phone) {
        String cleanPhone = phone != null ? phone.trim() : "";
        return customerRepository.findByPhone(cleanPhone)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Customer not found with phone number: " + cleanPhone));
    }

    public CustomerResponse getCustomerByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return customerRepository.findByCustomerCode(cleanCode)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("Customer not found with code: " + cleanCode));
    }

    public Page<CustomerResponse> listCustomers(String search, Pageable pageable) {
        return customerRepository.searchCustomers(search != null ? search.trim() : "", pageable)
                .map(CustomerResponse::from);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + id));

        if (request.phone() != null && !request.phone().isBlank()) {
            String newPhone = request.phone().trim();
            if (!newPhone.equals(customer.getPhone()) && customerRepository.existsByPhone(newPhone)) {
                throw new BusinessException("Phone number '" + newPhone + "' is already registered to another customer (BR-B01)");
            }
            customer.setPhone(newPhone);
        }

        customer.setFullName(request.fullName());
        customer.setAddress(request.address());
        customer.setNote(request.note());

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }

    @Transactional
    public CustomerResponse changeStatus(Long id, CustomerStatus status) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + id));

        if (status == CustomerStatus.INACTIVE) {
            customer.deactivate();
        } else if (status == CustomerStatus.ACTIVE) {
            customer.activate();
        }

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }
}
