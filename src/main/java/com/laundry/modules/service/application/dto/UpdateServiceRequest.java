package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.service.domain.ServiceUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record UpdateServiceRequest(
        @Size(max = 100, message = "Tên dịch vụ tối đa 100 ký tự")
        String name,

        ServiceUnit unit,

        @NotEmpty(message = "Vui lòng nhập giá theo hình thức giặt")
        @Valid
        List<CreateServiceRequest.PriceLine> prices,

        @DecimalMin(value = "0", message = "Tối thiểu không được âm")
        BigDecimal minQuantity,

        Boolean whitesApplicable,

        // Tùy chọn: đổi trạng thái cùng lúc với thông tin, khỏi gọi thêm PATCH /status
        ServiceStatus status,

        // Bắt buộc khi đổi đơn giá, phụ phí, thời gian xử lý hoặc trạng thái; AuditLogService kiểm tra
        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
        String priceChangeReason
) {}
