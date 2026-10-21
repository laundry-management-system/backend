package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Chi tiết một dòng đồ giặt theo BM3 và bảng order_items")
public record OrderItemDto(
        @Schema(description = "Mã định danh (nếu có)")
        Long id,

        @Schema(description = "Mã dịch vụ (tùy chọn)")
        Long serviceId,

        @NotBlank(message = "Tên loại đồ không được để trống")
        @Schema(description = "Tên loại đồ", example = "Áo sơ mi trắng")
        String itemType,

        @NotNull(message = "Số lượng/khối lượng không được để trống")
        @DecimalMin(value = "0.01", message = "Số lượng phải lớn hơn 0")
        @Schema(description = "Khối lượng (kg) hoặc số lượng chiếc", example = "3")
        BigDecimal quantity,

        @Schema(description = "Đơn vị tính (KG, PIECE, SET, PRODUCT)", example = "PIECE")
        ItemUnit unit,

        @Schema(description = "Đơn giá chụp tại thời điểm tạo đơn", example = "15000")
        BigDecimal unitPrice,

        @Schema(description = "Phụ phí giặt nhanh trên 1 đơn vị", example = "0")
        BigDecimal expressSurcharge,

        @Schema(description = "Thành tiền của dòng đồ", example = "45000")
        BigDecimal lineTotal,

        @Schema(description = "Ghi chú tình trạng món có vấn đề (vết ố, rách...)", example = "Ố nhẹ ở cổ áo")
        String conditionNote,

        @Schema(description = "Danh sách ảnh tình trạng món đồ")
        List<OrderImageDto> images
) {
    public static OrderItemDto from(OrderItem item) {
        List<OrderImageDto> imageDtos = item.getImages() != null
                ? item.getImages().stream().map(OrderImageDto::from).toList()
                : List.of();

        return new OrderItemDto(
                item.getId(),
                item.getServiceId(),
                item.getItemType(),
                item.getQuantity(),
                item.getUnit(),
                item.getUnitPrice(),
                item.getExpressSurcharge(),
                item.getLineTotal(),
                item.getConditionNote(),
                imageDtos
        );
    }
}
