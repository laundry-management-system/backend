package com.laundry.modules.customer.application.dto;

import com.laundry.modules.customer.domain.Customer;

/** Thông tin khách tự xem: không có ghi chú nội bộ, VIP hay người tạo. */
public record CustomerProfileResponse(
        String customerCode,
        String fullName,
        String phone,
        String email,
        String address
) {
    public static CustomerProfileResponse from(Customer c) {
        return new CustomerProfileResponse(c.getCustomerCode(), c.getFullName(), c.getPhone(), c.getEmail(), c.getAddress());
    }
}
