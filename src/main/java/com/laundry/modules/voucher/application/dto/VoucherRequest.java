package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dữ liệu tạo / sửa voucher (BM10)")
public record VoucherRequest(
        @NotBlank(message = "Vui lòng nhập mã voucher")
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$", message = "Mã voucher 3–30 ký tự, chỉ gồm chữ, số, '-' và '_'")
        @Schema(example = "GIAT10")
        String code,

        @NotNull(message = "Vui lòng chọn loại giảm giá")
        DiscountType discountType,

        @NotNull(message = "Vui lòng nhập giá trị giảm")
        @DecimalMin(value = "1", message = "Giá trị giảm phải lớn hơn 0")
        @Digits(integer = 12, fraction = 0, message = "Giá trị giảm phải là số nguyên")
        @Schema(description = "PERCENT: 1–100; AMOUNT: số tiền (đ)", example = "10")
        BigDecimal discountValue,

        @NotNull(message = "Vui lòng chọn ngày bắt đầu")
        @Schema(example = "2026-11-01")
        LocalDate startDate,

        @NotNull(message = "Vui lòng chọn ngày kết thúc")
        @Schema(example = "2026-12-31")
        LocalDate endDate,

        @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không được âm")
        @Digits(integer = 12, fraction = 0, message = "Giá trị đơn tối thiểu phải là số nguyên")
        @Schema(description = "So với tiền hàng; bỏ trống = 0", example = "100000")
        BigDecimal minOrderValue,

        @NotNull(message = "Vui lòng nhập số lần sử dụng tối đa")
        @Min(value = 1, message = "Số lần sử dụng tối đa phải lớn hơn 0")
        Integer maxUses,

        @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
        String description
) {
}
