package com.laundry.modules.incident.application.dto;

import com.laundry.modules.incident.domain.Incident;
import com.laundry.modules.incident.domain.IncidentResolution;
import com.laundry.modules.incident.domain.IncidentStatus;
import com.laundry.modules.incident.domain.IncidentType;
import com.laundry.modules.incident.domain.RedoStage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record IncidentResponse(
        Long id,
        String incidentCode,
        Long orderId,
        String orderCode,
        String customerName,
        String customerPhone,
        Long orderItemId,
        @Schema(description = "Tên dòng đồ liên quan")
        String itemType,
        IncidentType type,
        String typeDescription,
        String description,
        Long machineId,
        String machineCode,
        Long staffInvolvedId,
        String staffInvolvedName,
        @Schema(description = "Ảnh tình trạng hiện tại")
        List<String> imageUrls,
        @Schema(description = "Ảnh và ghi chú tình trạng lúc nhận đồ của dòng đồ liên quan, để đối chiếu (QĐ11)")
        List<String> originalImageUrls,
        String originalConditionNote,
        IncidentResolution resolution,
        String resolutionDescription,
        RedoStage redoStage,
        BigDecimal compensationAmount,
        IncidentStatus status,
        String statusDescription,
        Long createdBy,
        String createdByName,
        Long decidedBy,
        String decidedByName,
        Instant decidedAt,
        String decisionNote,
        Integer openedProcessingCount,
        Instant createdAt
) {
    /** Thông tin tham chiếu lấy từ các module khác. */
    public record Refs(String orderCode, String customerName, String customerPhone, String itemType,
                       List<String> originalImageUrls, String originalConditionNote, String machineCode,
                       String staffInvolvedName, String createdByName, String decidedByName) {
    }

    public static IncidentResponse from(Incident i, Refs r) {
        return new IncidentResponse(i.getId(), i.getIncidentCode(), i.getOrderId(), r.orderCode(), r.customerName(),
                r.customerPhone(), i.getOrderItemId(), r.itemType(), i.getType(), i.getType().getDescription(),
                i.getDescription(), i.getMachineId(), r.machineCode(), i.getStaffInvolvedId(), r.staffInvolvedName(),
                i.getImageUrls() == null ? List.of() : i.getImageUrls(), r.originalImageUrls(),
                r.originalConditionNote(), i.getResolution(), i.getResolution().getDescription(), i.getRedoStage(),
                i.getCompensationAmount(), i.getStatus(), i.getStatus().getDescription(), i.getCreatedBy(),
                r.createdByName(), i.getDecidedBy(), r.decidedByName(), i.getDecidedAt(), i.getDecisionNote(),
                i.getOpenedProcessingCount(), i.getCreatedAt());
    }
}
