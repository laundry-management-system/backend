package com.laundry.modules.machine.application.dto;

import com.laundry.modules.machine.domain.MachineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Dữ liệu thêm / sửa máy (BM8). Khi sửa, không đổi được loại máy")
public record MachineRequest(
        @NotBlank(message = "Vui lòng nhập tên máy")
        @Size(max = 100, message = "Tên máy tối đa 100 ký tự")
        @Schema(example = "Máy giặt cửa trước 10kg")
        String name,

        @NotNull(message = "Vui lòng chọn loại máy")
        MachineType type,

        @NotNull(message = "Vui lòng nhập công suất")
        @DecimalMin(value = "0.1", message = "Công suất phải lớn hơn 0")
        @Digits(integer = 4, fraction = 2, message = "Công suất tối đa 2 chữ số thập phân")
        @Schema(description = "Công suất (kg)", example = "10")
        BigDecimal capacityKg,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
}
