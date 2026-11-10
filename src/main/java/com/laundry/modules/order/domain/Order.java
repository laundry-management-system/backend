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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer_phone", columnList = "customerPhone"),
                @Index(name = "idx_orders_order_code", columnList = "orderCode"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_due_at", columnList = "dueAt"),
                @Index(name = "idx_orders_received_at", columnList = "received_at"),
                @Index(name = "idx_orders_intake_code", columnList = "intake_code")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_orders_intake_request_type",
                columnNames = {"intake_request_id", "order_type"})
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

    // Lượt nhận v3: null = đơn tạo trước tiếp nhận v3
    @Column(name = "intake_code", length = 20)
    private String intakeCode;

    @Column(name = "intake_request_id")
    private UUID intakeRequestId;

    // Dịch vụ chọn ở quầy: chỉ để tham khảo/gợi ý, không có giá
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requested_service_ids", columnDefinition = "jsonb")
    private List<Long> requestedServiceIds;

    @Column(name = "wash_whites_separately", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean washWhitesSeparately = false;

    // Chốt lúc phân loại
    @Column(name = "whites_fee", nullable = false, precision = 12, scale = 0, columnDefinition = "numeric(12,0) default 0")
    @Builder.Default
    private BigDecimal whitesFee = BigDecimal.ZERO;

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

    // Lần xử lý (BR-D17): tăng khi phiếu sự cố được duyệt mở lần xử lý mới
    @Column(name = "processing_count", nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private Integer processingCount = 1;

    // Phần giảm giá do phiếu sự cố (phương án DISCOUNT); nằm trong discountAmount cùng với voucher
    @Column(name = "incident_discount", nullable = false, precision = 12, scale = 0, columnDefinition = "numeric(12,0) default 0")
    @Builder.Default
    private BigDecimal incidentDiscount = BigDecimal.ZERO;

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

    public boolean isIntakeV3() {
        return this.intakeCode != null;
    }

    /**
     * Tổng theo QĐ5: Σ thành tiền + phụ phí nhanh + phụ phí đồ trắng − giảm giá (giảm ≤ tiền hàng).
     * {@code requestedDiscount} là phần giảm của voucher; phần giảm theo phiếu sự cố được cộng thêm.
     */
    public void recalcTotals(BigDecimal requestedDiscount) {
        BigDecimal incident = this.incidentDiscount == null ? BigDecimal.ZERO : this.incidentDiscount;
        requestedDiscount = (requestedDiscount == null ? BigDecimal.ZERO : requestedDiscount).add(incident);
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal express = BigDecimal.ZERO;
        for (OrderItem item : this.items) {
            sub = sub.add(item.getLineTotal());
            if (this.orderType == OrderType.EXPRESS) {
                express = express.add(item.getExpressSurcharge()
                        .multiply(item.getBillableQuantity())
                        .setScale(0, java.math.RoundingMode.HALF_UP));
            }
        }
        BigDecimal discount = requestedDiscount == null ? BigDecimal.ZERO : requestedDiscount.min(sub);
        this.subtotal = sub;
        this.expressFee = express;
        this.discountAmount = discount;
        this.totalAmount = sub.add(express).add(this.whitesFee).subtract(discount);
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

    /**
     * Chỉ dùng cho bước Mới nhận → Đang xử lý. Hoàn thành, giao đồ và hủy có API riêng kèm kiểm tra
     * (xếp kệ, thanh toán, BR-D05), nên không được đi tắt qua đây.
     */
    public void changeStatus(OrderStatus newStatus, String reason, String performedBy) {
        if (this.status.isTerminal()) {
            throw new BusinessException("Đơn hàng đang ở trạng thái '" + this.status.getDescription() + "', không thể chuyển tiếp");
        }
        if (this.status == newStatus) {
            throw new BusinessException("Đơn hàng đã ở trạng thái '" + newStatus.getDescription() + "'");
        }
        if (newStatus != OrderStatus.PROCESSING || this.status != OrderStatus.RECEIVED) {
            throw new BusinessException(switch (newStatus) {
                case COMPLETED -> "Dùng chức năng Hoàn thành (kèm vị trí kệ) để chuyển đơn sang Hoàn thành";
                case DELIVERED -> "Dùng chức năng Giao đồ / Thanh toán & giao để giao đơn";
                case CANCELLED -> "Dùng chức năng Hủy đơn để hủy";
                default -> "Chỉ chuyển được từ Mới nhận sang Đang xử lý; muốn lùi trạng thái hãy dùng chức năng Lùi trạng thái";
            }, "newStatus");
        }
        if (isIntakeV3() && this.items.isEmpty()) {
            throw new BusinessException("Đơn chưa phân loại (chưa có dòng đồ và giá), không thể bắt đầu xử lý");
        }
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
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

    public void revertStatus(OrderStatus targetStatus, String reason, String performedBy) {
        if (this.status.isTerminal()) {
            throw new BusinessException("Không thể lùi trạng thái của đơn hàng đã kết thúc");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do khi lùi trạng thái đơn hàng (BR-D06)");
        }
        // Chỉ lùi về công đoạn trước trong luồng Mới nhận → Đang xử lý → Hoàn thành
        List<OrderStatus> flow = OrderStatus.getStandardFlow();
        if (!flow.contains(targetStatus) || flow.indexOf(targetStatus) >= flow.indexOf(this.status)) {
            throw new BusinessException("Chỉ lùi được về trạng thái trước trạng thái hiện tại ("
                    + this.status.getDescription() + ")", "targetStatus");
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
        if (this.status == OrderStatus.CANCELLED) {
            throw new BusinessException("Không thể sửa giá đơn hàng đã hủy");
        }
        if (this.paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("Đơn đã thanh toán: hủy xác nhận thanh toán trước khi sửa giá");
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

    /** BR-D05: chỉ hủy được đơn Mới nhận. Hoàn tiền và trả lượt voucher do OrderService xử lý trước khi gọi. */
    public void cancel(String reason, String performedBy) {
        if (this.status == OrderStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng này đã bị hủy trước đó");
        }
        if (this.status != OrderStatus.RECEIVED) {
            throw new BusinessException("Chỉ hủy được đơn ở trạng thái Mới nhận (BR-D05); đơn đang "
                    + this.status.getDescription());
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do hủy đơn hàng (BR-D05)");
        }
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.CANCELLED;
        addHistory(HistoryActionType.CANCELLED, oldStatus.name(), OrderStatus.CANCELLED.name(), reason, performedBy);
    }

    /** Phiếu sự cố phương án DISCOUNT được duyệt: trừ vào đơn chưa thanh toán. */
    public void applyIncidentDiscount(BigDecimal amount, String incidentCode, String performedBy) {
        if (this.status.isTerminal()) {
            throw new BusinessException("Đơn đã kết thúc, không giảm giá được");
        }
        if (this.paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("Đơn đã thanh toán: chọn phương án Bồi thường thay vì Giảm giá");
        }
        if (amount == null || amount.signum() <= 0 || amount.compareTo(this.totalAmount) > 0) {
            throw new BusinessException("Số tiền giảm phải lớn hơn 0 và không vượt tổng tiền đơn ("
                    + this.totalAmount.toPlainString() + "đ)");
        }
        BigDecimal oldTotal = this.totalAmount;
        this.incidentDiscount = this.incidentDiscount.add(amount);
        this.discountAmount = (this.discountAmount == null ? BigDecimal.ZERO : this.discountAmount).add(amount);
        this.totalAmount = this.totalAmount.subtract(amount);
        addHistory(HistoryActionType.PRICE_ADJUST, oldTotal.toPlainString(), this.totalAmount.toPlainString(),
                "Giảm giá theo phiếu sự cố " + incidentCode, performedBy);
    }

    /** Phiếu sự cố phương án REWASH được duyệt: mở lần xử lý mới (BR-D17), đơn quay về Đang xử lý. */
    public void reopenForIncident(String incidentCode, String redoStage, String performedBy) {
        if (this.status != OrderStatus.PROCESSING && this.status != OrderStatus.COMPLETED) {
            throw new BusinessException("Chỉ mở lần xử lý mới cho đơn Đang xử lý hoặc Hoàn thành (chưa giao); đơn đang "
                    + this.status.getDescription());
        }
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.PROCESSING;
        this.completedAt = null;
        this.processingCount = (this.processingCount == null ? 1 : this.processingCount) + 1;
        addHistory(HistoryActionType.INCIDENT_REOPEN, oldStatus.name(), OrderStatus.PROCESSING.name(),
                "Phiếu sự cố " + incidentCode + ": làm lại công đoạn " + redoStage
                        + " · lần xử lý " + this.processingCount, performedBy);
    }

    /** Ghi nhận đã hoàn tiền giao dịch của đơn (khi hủy đơn đã thanh toán). */
    public void markRefunded(String reason, String performedBy) {
        if (this.paymentStatus != PaymentStatus.PAID) return;
        this.paymentStatus = PaymentStatus.UNPAID;
        addHistory(HistoryActionType.PAYMENT_VOIDED, "CONFIRMED", "REFUNDED", reason, performedBy);
    }

    public void markReceiptPrinted() {
        if (this.receiptPrintedAt == null) {
            this.receiptPrintedAt = Instant.now();
        }
    }
}
