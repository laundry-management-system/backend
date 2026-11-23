package com.laundry.modules.report.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laundry.modules.report.domain.Granularity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Báo cáo vận hành: đơn theo nhân viên, ca làm, sự cố")
public record OperationsReportResponse(
        LocalDate from,
        LocalDate to,
        Granularity granularity,

        @Schema(description = "Đơn theo nhân viên, sắp orders giảm dần")
        List<StaffOrders> byStaff,

        Shifts shifts,

        @Schema(description = "Các ca đã đếm trong shifts, sắp closedAt tăng dần")
        List<ShiftRow> shiftRows,

        Incidents incidents,

        Previous previous
) {

    @Schema(description = "Đơn của một nhân viên")
    public record StaffOrders(
            String username,
            @Schema(description = "null khi username không còn trong users")
            String fullName,
            long orders
    ) {}

    @Schema(description = "Tổng hợp ca có closed_at trong kỳ")
    public record Shifts(long closed, @JsonProperty("short") long shortCount, long over, BigDecimal differenceTotal, BigDecimal handInTotal) {}

    public record ShiftRow(Long shiftId, String code, String openedByName, Instant closedAt, String status,
                           BigDecimal difference, BigDecimal handInCash) {}

    @Schema(description = "Sự cố tạo trong kỳ")
    public record Incidents(
            long total,
            List<PeriodCount> rows,
            @Schema(description = "Sắp count giảm dần")
            List<TypeCount> byType,
            @Schema(description = "Sắp count giảm dần")
            List<StatusCount> byStatus,
            Payouts payouts
    ) {}

    public record PeriodCount(String period, long count) {}

    public record TypeCount(String type, long count) {}

    public record StatusCount(String status, long count) {}

    @Schema(description = "Tiền chi sự cố theo paid_at trong kỳ")
    public record Payouts(BigDecimal cash, BigDecimal bankTransfer) {}

    @Schema(description = "Số liệu kỳ liền trước")
    public record Previous(long incidentsTotal) {}
}
