package com.laundry.modules.customer.application.dto;

import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerRegistration;
import com.laundry.modules.customer.domain.CustomerStatus;
import java.time.Instant;

public record RegistrationResponse(
        Long id,
        String fullName,
        String phone,
        String email,
        Instant createdAt,
        Long matchedCustomerId,
        String matchedCustomerCode,
        CustomerStatus matchedCustomerStatus
) {
    public static RegistrationResponse from(CustomerRegistration r, Customer matched) {
        return new RegistrationResponse(
                r.getId(), r.getFullName(), r.getPhone(), r.getEmail(), r.getCreatedAt(),
                matched != null ? matched.getId() : null,
                matched != null ? matched.getCustomerCode() : null,
                matched != null ? matched.getStatus() : null);
    }
}
