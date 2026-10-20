package com.laundry.modules.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request giao đồ cho khách hoặc người nhận hộ (QĐ9)")
public record DeliverOrderRequest(
        @Schema(description = "Họ tên người nhận hộ (nếu nhận hộ)", example = "Nguyễn Văn B")
        String receiverName,

        @Schema(description = "Mối quan hệ với khách hàng (nếu nhận hộ)", example = "Anh trai")
        String receiverRelation
) {
}

