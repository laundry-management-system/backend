package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.service.domain.ServiceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateServiceRequest(
        @Size(max = 100, message = "Tên dịch vụ tối đa 100 ký tự")
        String name,

        ServiceUnit unit,

        @DecimalMin(value = "0", message = "Đơn giá không được âm")
        BigDecimal currentPrice,

        @Min(value = 1, message = "Thời gian xử lý chuẩn phải lớn hơn 0 giờ")
        Integer standardHours,

        Boolean expressSupported,

        @DecimalMin(value = "0", message = "Phụ phí giặt nhanh không được âm")
        BigDecimal expressSurcharge,

        @Min(value = 1, message = "Thời gian giặt nhanh phải lớn hơn 0 giờ")
        Integer expressHours,

        // Tùy chọn: đổi trạng thái cùng lúc với thông tin, khỏi gọi thêm PATCH /status
        ServiceStatus status
) {}
