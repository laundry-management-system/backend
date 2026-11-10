package com.laundry.modules.voucher.application.dto;

import com.laundry.modules.voucher.domain.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Sửa voucher. Không đổi được mã; voucher đã có đơn dùng thì không đổi được loại giảm giá")
public record UpdateVoucherRequest(
        @NotNull(message = "Vui lòng chọn loại giảm giá")
        DiscountType discountType,

        @NotNull(message = "Vui lòng nhập giá trị giảm")
        @DecimalMin(value = "1", message = "Giá trị giảm phải lớn hơn 0")
        @Digits(integer = 12, fraction = 0, message = "Giá trị giảm phải là số nguyên")
        BigDecimal discountValue,

        @NotNull(message = "Vui lòng chọn ngày bắt đầu (BR-G01)")
        LocalDate startDate,

        @NotNull(message = "Vui lòng chọn ngày kết thúc (BR-G01)")
        LocalDate endDate,

        @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không được âm (BR-G02)")
        @Digits(integer = 12, fraction = 0, message = "Giá trị đơn tối thiểu phải là số nguyên")
        BigDecimal minOrderValue,

        @NotNull(message = "Vui lòng nhập số lần sử dụng tối đa (BR-G03)")
        @Min(value = 1, message = "Số lần sử dụng tối đa phải lớn hơn 0 (BR-G03)")
        Integer maxUses,

        @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
        String description
) {
}
