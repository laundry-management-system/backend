package com.laundry.modules.order.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "item_type", nullable = false, length = 100)
    private String itemType;

    @Column(nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ItemUnit unit = ItemUnit.PIECE;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "express_surcharge", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal expressSurcharge = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    // Chốt từ dịch vụ lúc phân loại (đơn v3); null với đơn cũ
    @Column(name = "min_quantity", precision = 10, scale = 2)
    private BigDecimal minQuantity;

    @Column(name = "actual_quantity", precision = 10, scale = 2)
    private BigDecimal actualQuantity;

    @Column(name = "billable_quantity", precision = 10, scale = 2)
    private BigDecimal billableQuantity;

    @Column(name = "condition_note", length = 500)
    private String conditionNote;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderImage> images = new ArrayList<>();

    /** KL tính tiền = max(thực tế, tối thiểu); thành tiền = đơn giá × KL tính tiền. */
    public void bill(BigDecimal actual) {
        BigDecimal min = this.minQuantity != null ? this.minQuantity : BigDecimal.ZERO;
        this.actualQuantity = actual;
        this.billableQuantity = actual.max(min);
        this.quantity = this.billableQuantity;
        this.lineTotal = this.unitPrice.multiply(this.billableQuantity).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    public void addImage(String imageUrl, String note) {
        OrderImage image = OrderImage.builder()
                .orderItem(this)
                .imageUrl(imageUrl)
                .note(note)
                .build();
        this.images.add(image);
    }
}
