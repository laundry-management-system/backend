package com.laundry.modules.customers.application.dto;

import com.laundry.modules.customers.domain.Customer;
import com.laundry.modules.customers.domain.CustomerStatus;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String customerCode,
        String fullName,
        String phone,
        String address,
        String note,
        boolean vip,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getCustomerCode(), customer.getFullName(),
                customer.getPhone(), customer.getAddress(), customer.getNote(), customer.isVip(),
                customer.getStatus(), customer.getCreatedAt(), customer.getUpdatedAt());
    }
}
