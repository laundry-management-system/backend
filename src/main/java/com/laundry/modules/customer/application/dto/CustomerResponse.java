package com.laundry.modules.customer.application.dto;

import com.laundry.modules.customer.domain.Customer;
import com.laundry.modules.customer.domain.CustomerStatus;
import com.laundry.modules.customer.infrastructure.CustomerOrderStats;
import io.swagger.v3.oas.annotations.media.Schema;

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
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        @Schema(description = "Tổng số đơn của khách")
        long orderCount,
        @Schema(description = "Tổng tiền các đơn đã thanh toán, không tính đơn hủy")
        BigDecimal totalSpent,
        @Schema(description = "Thời điểm nhận đơn gần nhất")
        Instant lastOrderAt
) {
    public static CustomerResponse from(Customer customer, CustomerOrderStats.Stats stats) {
        CustomerOrderStats.Stats s = stats == null ? CustomerOrderStats.Stats.EMPTY : stats;
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
                customer.getUpdatedAt(),
                s.orderCount(),
                s.totalSpent(),
                s.lastOrderAt()
        );
    }
}
