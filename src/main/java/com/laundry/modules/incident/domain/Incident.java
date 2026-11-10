package com.laundry.modules.incident.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Phiếu sự cố và bồi thường (BM9 / QĐ11, BR-D17). NV lập, Admin duyệt trước khi xử lý. */
@Entity
@Table(
        name = "incidents",
        indexes = {
                @Index(name = "idx_incidents_order", columnList = "order_id"),
                @Index(name = "idx_incidents_status", columnList = "status"),
                @Index(name = "idx_incidents_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Incident extends BaseEntity {

    @Column(name = "incident_code", nullable = false, unique = true, length = 20)
    private String incidentCode;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IncidentType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "staff_involved_id")
    private Long staffInvolvedId;

    /** Ảnh hiện tại của món đồ (id file trong module document). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_ids", columnDefinition = "jsonb")
    @Builder.Default
    private List<Long> documentIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private IncidentResolution resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "redo_stage", nullable = false, length = 10)
    @Builder.Default
    private RedoStage redoStage = RedoStage.NONE;

    /** Số tiền giảm (DISCOUNT) hoặc bồi thường (COMPENSATE); 0 với REWASH. */
    @Column(name = "compensation_amount", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal compensationAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.PENDING;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Ghi chú khi duyệt hoặc lý do từ chối. */
    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    /** Lần xử lý được mở khi duyệt phương án REWASH. */
    @Column(name = "opened_processing_count")
    private Integer openedProcessingCount;

    public void approve(Long adminId, String note) {
        requirePending();
        this.status = IncidentStatus.APPROVED;
        this.decidedBy = adminId;
        this.decidedAt = Instant.now();
        this.decisionNote = note;
    }

    public void reject(Long adminId, String reason) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc nhập lý do từ chối", "reason");
        }
        this.status = IncidentStatus.REJECTED;
        this.decidedBy = adminId;
        this.decidedAt = Instant.now();
        this.decisionNote = reason.trim();
    }

    private void requirePending() {
        if (status != IncidentStatus.PENDING) {
            throw new BusinessException("Phiếu sự cố đã được xử lý (" + status.getDescription() + ")");
        }
    }
}
