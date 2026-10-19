package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.OrderPriority;
import com.laundry.modules.order.domain.OrderType;
import com.laundry.modules.order.domain.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request tạo đơn nhanh tại quầy (Fast Check-in 15-30 giây - BM3)")
public record QuickCreateOrderRequest(
        @NotBlank(message = "Tên khách hàng không được để trống")
        @Schema(description = "Tên khách hàng", example = "Nguyễn Văn A")
        String customerName,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng Việt Nam")
        @Schema(description = "Số điện thoại khách hàng", example = "0987654321")
        String customerPhone,

        @NotNull(message = "Loại hình giặt không được để trống")
        @Schema(description = "Loại hình giặt sấy", example = "WASH_DRY")
        ServiceType serviceType,

        @NotNull(message = "Số kg đồ không được để trống")
        @DecimalMin(value = "0.01", message = "Số kg phải lớn hơn 0")
        @Schema(description = "Số kg đồ sau khi cân cả túi", example = "5.5")
        BigDecimal totalWeight,

        @Schema(description = "Đơn giá theo kg (nếu để trống, hệ thống tự lấy giá mặc định của loại dịch vụ)", example = "15000")
        BigDecimal unitPrice,

        @Schema(description = "Ghi chú dặn dò của khách (VD: áo trắng giặt riêng)", example = "Áo trắng giặt riêng, không dùng xả thơm")
        String customerNote,

        @Schema(description = "Độ ưu tiên xử lý (NORMAL, HIGH, URGENT)", example = "NORMAL")
        OrderPriority priority,

        @Schema(description = "Loại đơn (NORMAL, EXPRESS)", example = "NORMAL")
        OrderType orderType,

        @Schema(description = "Số túi đồ", example = "1")
        Integer bagCount,

        @Schema(description = "Ngày hẹn trả khách (nếu để trống, hệ thống tự động gợi ý theo loại dịch vụ)")
        Instant dueAt
) {
}
