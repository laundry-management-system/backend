package com.laundry.modules.customer.application.port;

import java.math.BigDecimal;
import java.time.Instant;

/** Thống kê đơn của một khách; totalSpent chỉ tính đơn đã thanh toán, không bị hủy. */
public record CustomerOrderStats(long orderCount, BigDecimal totalSpent, Instant lastOrderAt) {

    public static final CustomerOrderStats EMPTY = new CustomerOrderStats(0, BigDecimal.ZERO, null);
}
