package com.laundry.modules.report.application.dto;

import com.laundry.modules.report.domain.ReportQueryRepository.RevenuePaymentRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Một giao dịch đã xác nhận, được tính vào doanh thu")
public record RevenuePaymentResponse(
        Long paymentId,
        Instant paidAt,
        @Schema(description = "CASH hoặc BANK_TRANSFER") String method,
        BigDecimal amount,
        String invoiceNo,
        Long orderId,
        String orderCode,
        String customerName,
        @Schema(description = "Kênh của đơn: ONLINE hoặc OFFLINE") String channel
) {
    public static RevenuePaymentResponse from(RevenuePaymentRecord r) {
        return new RevenuePaymentResponse(r.paymentId(), r.paidAt(), r.method(), r.amount(),
                r.invoiceNo(), r.orderId(), r.orderCode(), r.customerName(), r.channel());
    }
}
