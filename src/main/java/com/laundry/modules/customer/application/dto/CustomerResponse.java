package com.laundry.modules.customer.application.dto;

import com.laundry.modules.customer.application.port.CustomerOrderStats;
import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerSource;
import com.laundry.modules.customer.domain.CustomerStatus;
import java.math.BigDecimal;
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
        String email,
        CustomerSource source,
        boolean hasAccount,
        Long pendingRegistrationId,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Long orderCount,
        BigDecimal totalSpent,
        Instant lastOrderAt
) {
    public static CustomerResponse from(Customer customer) {
        return from(customer, null, null);
    }

    /**
     * pendingRegistrationId: yêu cầu đăng ký đang chờ Admin duyệt của hồ sơ, null nếu không có.
     * stats: null khi endpoint không tính thống kê, khi đó ba trường thống kê là null.
     */
    public static CustomerResponse from(Customer customer, Long pendingRegistrationId, CustomerOrderStats stats) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getNote(),
                customer.getIsVip(),
                customer.getStatus(),
                customer.getEmail(),
                customer.getSource(),
                customer.hasAccount(),
                pendingRegistrationId,
                customer.getCreatedBy(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                stats == null ? null : stats.orderCount(),
                stats == null ? null : stats.totalSpent(),
                stats == null ? null : stats.lastOrderAt()
        );
    }
}
