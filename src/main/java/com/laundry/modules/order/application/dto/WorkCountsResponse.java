package com.laundry.modules.order.application.dto;

import com.laundry.modules.order.domain.WorkCounts;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Số đơn Mới nhận + Đang xử lý theo tab của Danh sách công việc; các tab chồng nhau")
public record WorkCountsResponse(
        @Schema(description = "Mới nhận + Đang xử lý") long all,
        @Schema(description = "Mới nhận chưa có dòng đồ (tiếp nhận nhanh)") long unclassified,
        @Schema(description = "Mới nhận, gồm cả chưa phân loại") long received,
        @Schema(description = "Đang xử lý") long processing,
        @Schema(description = "Đã quá giờ hẹn trả") long late,
        @Schema(description = "Giặt nhanh") long express,
        @Schema(description = "Chờ xác nhận (đơn online)") long pendingConfirmation,
        @Schema(description = "Chờ nhận đồ (đơn online)") long awaitingDropoff
) {
    public static WorkCountsResponse from(WorkCounts c) {
        return new WorkCountsResponse(c.getTotal(), c.getUnclassified(), c.getReceived(),
                c.getProcessing(), c.getLate(), c.getExpress(),
                c.getPendingConfirmation(), c.getAwaitingDropoff());
    }
}
