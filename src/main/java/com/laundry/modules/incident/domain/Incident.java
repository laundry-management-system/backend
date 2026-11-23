package com.laundry.modules.incident.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.ResourceConflictException;
import com.laundry.modules.order.domain.ItemUnit;
import com.laundry.modules.order.domain.Order;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phiếu sự cố & bồi thường (BM9). Không tự đổi trạng thái đơn (BR-I03). */
@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_order", columnList = "order_id"),
        @Index(name = "idx_incidents_status_created", columnList = "status, created_at"),
        @Index(name = "idx_incidents_payout_shift", columnList = "payout_shift_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Incident extends BaseEntity {

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Không khóa ngoại: lưu phân loại xóa rồi tạo lại dòng đồ, nên phiếu giữ bản chụp dòng bên dưới
    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "item_label", length = 100)
    private String itemLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_unit", length = 20)
    private ItemUnit itemUnit;

    @Column(name = "item_quantity", precision = 10, scale = 2)
    private BigDecimal itemQuantity;

    @Column(name = "unit_price", precision = 12, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 12, scale = 0)
    private BigDecimal lineTotal;

    @Column(name = "affected_count", nullable = false)
    @Builder.Default
    private int affectedCount = 1;

    @Column(name = "max_compensation", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal maxCompensation = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IncidentType type;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private IncidentResolution resolution;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.PENDING;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "over_limit_reason", length = 500)
    private String overLimitReason;

    @Column(name = "deducted_amount", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal deductedAmount = BigDecimal.ZERO;

    @Column(name = "payout_due", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal payoutDue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", length = 20)
    private PayoutMethod payoutMethod;

    @Column(name = "payout_amount", precision = 12, scale = 0)
    private BigDecimal payoutAmount;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "paid_by", length = 50)
    private String paidBy;

    @Column(name = "payout_shift_id")
    private Long payoutShiftId;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<IncidentImage> images = new ArrayList<>();

    public boolean isPending() {
        return this.status == IncidentStatus.PENDING;
    }

    public void requirePending() {
        if (!isPending()) {
            throw new ResourceConflictException("Phiếu đã được xử lý, không sửa được");
        }
    }

    public void replaceImages(List<IncidentImage> next) {
        this.images.clear();
        for (IncidentImage image : next) {
            image.setIncident(this);
            this.images.add(image);
        }
    }
}
