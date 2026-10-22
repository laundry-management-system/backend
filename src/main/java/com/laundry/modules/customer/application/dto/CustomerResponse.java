package com.laundry.modules.customer.application.dto;

import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerStatus;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String customerCode,
        String fullName,
        String phone,
        String address,
        String note,
        Boolean isVip,
        CustomerStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getNote(),
                customer.getIsVip(),
                customer.getStatus(),
                customer.getCreatedBy(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
