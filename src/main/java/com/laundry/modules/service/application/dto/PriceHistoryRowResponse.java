package com.laundry.modules.service.application.dto;

import com.laundry.modules.service.domain.PriceField;
import com.laundry.modules.service.domain.ServiceUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Một trường đã đổi trong một lần sửa dịch vụ (lịch sử thay đổi giá)")
public record PriceHistoryRowResponse(
        @Schema(description = "Id bản ghi nhật ký; các trường đổi cùng lần lưu có chung id", example = "812")
        Long logId,
        Instant changedAt,
        Long serviceId,
        @Schema(description = "Mã dịch vụ hiện tại", example = "DV02")
        String serviceCode,
        @Schema(description = "Tên dịch vụ hiện tại", example = "Giặt sấy")
        String serviceName,
        ServiceUnit unit,
        PriceField field,
        @Schema(description = "Id hình thức giặt khi field = MODE_PRICE, còn lại null", example = "2")
        Long washModeId,
        @Schema(description = "Tên hình thức giặt hiện tại khi field = MODE_PRICE, còn lại null", example = "Giặt nhanh")
        String washModeName,
        @Schema(description = "Giá trị cũ dạng chuỗi (tiền, số giờ, tên trạng thái); null khi không có (MODE_PRICE: null = không hỗ trợ)", example = "25000")
        String oldValue,
        @Schema(example = "28000")
        String newValue,
        Long changedById,
        String changedByName,
        String reason
) {}
