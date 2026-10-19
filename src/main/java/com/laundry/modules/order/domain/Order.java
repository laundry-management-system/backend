package com.laundry.modules.order.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer_phone", columnList = "customerPhone"),
                @Index(name = "idx_orders_order_code", columnList = "orderCode"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_due_at", columnList = "dueAt")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "order_code", nullable = false, unique = true, length = 15)
    private String orderCode;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    @Builder.Default
    private OrderType orderType = OrderType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 30)
    private ServiceType serviceType;

    @Column(name = "total_weight", precision = 6, scale = 2)
    private BigDecimal totalWeight;

    @Column(name = "unit_price", precision = 12, scale = 0)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "express_fee", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal expressFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "bag_count", nullable = false)
    @Builder.Default
    private Integer bagCount = 1;

    @Column(name = "shelf_location", length = 20)
    private String shelfLocation;

    @Column(name = "receipt_printed_at")
    private Instant receiptPrintedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "delivered_by", length = 50)
    private String deliveredBy;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_relation", length = 50)
    private String receiverRelation;

    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    @Column(name = "notify_count", nullable = false)
    @Builder.Default
    private Integer notifyCount = 0;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderPriority priority = OrderPriority.NORMAL;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<OrderStatusHistory> historyList = new ArrayList<>();

    public boolean isOverdue() {
        return (this.status == OrderStatus.RECEIVED || this.status == OrderStatus.PROCESSING)
                && Instant.now().isAfter(this.dueAt);
    }

    public void addHistory(HistoryActionType actionType, String oldValue, String newValue, String reason, String performedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(this)
                .actionType(actionType)
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .build();
        this.historyList.add(history);
    }

    public void updateItems(List<OrderItem> newItems, String performedBy) {
        if (this.status.isTerminal()) {
            throw new BusinessException("Không thể cập nhật danh sách đồ cho đơn hàng đã kết thúc hoặc đã hủy");
        }
        this.items.clear();
        for (OrderItem item : newItems) {
            item.setOrder(this);
            this.items.add(item);
        }
        int totalPieces = this.items.size();
        addHistory(HistoryActionType.ITEMS_UPDATED, null, totalPieces + " dòng món", "Nhân viên kiểm đếm và phân loại chi tiết", performedBy);
    }

    public void changeStatus(OrderStatus newStatus, String reason, String performedBy) {
        if (this.status.isTerminal()) {
            throw new BusinessException("Đơn hàng đang ở trạng thái '" + this.status.getDescription() + "', không thể chuyển tiếp");
        }
        if (this.status == newStatus) {
            throw new BusinessException("Đơn hàng đã ở trạng thái '" + newStatus.getDescription() + "'");
        }
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        if (newStatus == OrderStatus.COMPLETED) {
            this.completedAt = Instant.now();
        } else if (newStatus == OrderStatus.DELIVERED) {
            this.deliveredAt = Instant.now();
            this.deliveredBy = performedBy;
        }
        addHistory(HistoryActionType.STATUS_CHANGE, oldStatus.name(), newStatus.name(), reason, performedBy);
    }

    public void complete(String shelfLocation, String performedBy) {
        if (this.status == OrderStatus.CANCELLED || this.status == OrderStatus.DELIVERED) {
            throw new BusinessException("Đơn hàng đã kết thúc không thể chuyển sang Hoàn thành");
        }
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.COMPLETED;
        this.shelfLocation = shelfLocation != null ? shelfLocation.trim() : null;
        this.completedAt = Instant.now();
        addHistory(HistoryActionType.STATUS_CHANGE, oldStatus.name(), OrderStatus.COMPLETED.name(),
                "Hoàn thành xử lý, xếp lên kệ: " + (this.shelfLocation != null ? this.shelfLocation : "Chưa gắn kệ"),
                performedBy);
    }

    public void deliver(String receiverName, String receiverRelation, String performedBy) {
        if (this.status != OrderStatus.COMPLETED) {
            throw new BusinessException("Đơn hàng phải ở trạng thái 'Hoàn thành / Chờ lấy' mới có thể giao đồ");
        }
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        this.deliveredBy = performedBy;
        this.receiverName = receiverName != null ? receiverName.trim() : null;
        this.receiverRelation = receiverRelation != null ? receiverRelation.trim() : null;
        String note = (this.receiverName != null && !this.receiverName.isBlank())
                ? "Giao cho người nhận hộ: " + this.receiverName + " (" + this.receiverRelation + ")"
                : "Giao đồ trực tiếp cho khách hàng";
        addHistory(HistoryActionType.STATUS_CHANGE, oldStatus.name(), OrderStatus.DELIVERED.name(), note, performedBy);
    }

    public void revertStatus(OrderStatus targetStatus, String reason, String performedBy) {
        if (this.status.isTerminal()) {
            throw new BusinessException("Không thể lùi trạng thái của đơn hàng đã kết thúc");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do khi lùi trạng thái đơn hàng (BR-D06)");
        }
        OrderStatus oldStatus = this.status;
        this.status = targetStatus;
        if (targetStatus != OrderStatus.COMPLETED) {
            this.completedAt = null;
        }
        addHistory(HistoryActionType.REVERT_STATUS, oldStatus.name(), targetStatus.name(), reason, performedBy);
    }

    public void reschedule(Instant newDueAt, String reason, String performedBy) {
        if (this.status.isTerminal()) {
            throw new BusinessException("Không thể dời lịch hẹn của đơn hàng đã hoàn tất hoặc đã hủy");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do dời ngày hẹn (BR-D08)");
        }
        Instant oldDueAt = this.dueAt;
        this.dueAt = newDueAt;
        addHistory(HistoryActionType.RESCHEDULE, String.valueOf(oldDueAt), String.valueOf(newDueAt), reason, performedBy);
    }

    public void adjustPrice(BigDecimal newAmount, String reason, String performedBy) {
        if (this.status == OrderStatus.DELIVERED) {
            throw new BusinessException("Không thể sửa giá đơn hàng đã giao (BR-D12)");
        }
        if (newAmount == null || newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Giá tiền điều chỉnh không hợp lệ");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do điều chỉnh giá");
        }
        BigDecimal oldAmount = this.totalAmount;
        this.totalAmount = newAmount;
        addHistory(HistoryActionType.PRICE_ADJUST, String.valueOf(oldAmount), String.valueOf(newAmount), reason, performedBy);
    }

    public void cancel(String reason, String performedBy) {
        if (this.status == OrderStatus.DELIVERED) {
            throw new BusinessException("Không thể hủy đơn hàng đã giao");
        }
        if (this.status == OrderStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng này đã bị hủy trước đó");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do hủy đơn hàng (BR-D05)");
        }
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.CANCELLED;
        addHistory(HistoryActionType.CANCELLED, oldStatus.name(), OrderStatus.CANCELLED.name(), reason, performedBy);
    }

    public void markReceiptPrinted() {
        if (this.receiptPrintedAt == null) {
            this.receiptPrintedAt = Instant.now();
        }
    }
}
