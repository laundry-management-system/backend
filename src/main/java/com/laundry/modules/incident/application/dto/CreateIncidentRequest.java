package com.laundry.modules.incident.application.dto;

import com.laundry.modules.incident.domain.IncidentResolution;
import com.laundry.modules.incident.domain.IncidentType;
import com.laundry.modules.incident.domain.RedoStage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Lập phiếu sự cố (BM9). Phiếu ở trạng thái Chờ duyệt cho đến khi Admin duyệt")
public record CreateIncidentRequest(
        @NotNull(message = "Thiếu đơn hàng")
        Long orderId,

        @Schema(description = "Dòng đồ liên quan (nếu xác định được)")
        Long orderItemId,

        @NotNull(message = "Vui lòng chọn loại sự cố")
        IncidentType type,

        @NotBlank(message = "Vui lòng mô tả sự cố")
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        @Schema(description = "Máy liên quan (nếu có)")
        Long machineId,

        @Schema(description = "Nhân viên liên quan (id tài khoản, nếu có)")
        Long staffInvolvedId,

        @Size(max = 10, message = "Tối đa 10 ảnh")
        @Schema(description = "Ảnh tình trạng hiện tại (id file đã upload qua /api/documents)")
        List<@NotNull Long> documentIds,

        @NotNull(message = "Vui lòng chọn phương án xử lý")
        IncidentResolution resolution,

        @Schema(description = "Bắt buộc khi phương án là REWASH (WASH / DRY / BOTH)")
        RedoStage redoStage,

        @DecimalMin(value = "0", message = "Số tiền không được âm")
        @Digits(integer = 12, fraction = 0, message = "Số tiền phải là số nguyên")
        @Schema(description = "Bắt buộc khi phương án là DISCOUNT hoặc COMPENSATE")
        BigDecimal compensationAmount
) {
}
