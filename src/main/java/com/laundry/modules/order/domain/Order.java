package com.laundry.modules.order.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer_phone", columnList = "customerPhone"),
                @Index(name = "idx_orders_order_code", columnList = "orderCode"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_due_at", columnList = "dueAt"),
                @Index(name = "idx_orders_intake_code", columnList = "intake_code"),
                @Index(name = "idx_orders_channel", columnList = "channel")
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

    /** Định dạng hạn trả trong thông báo lỗi (giờ VN). */
    public static final DateTimeFormatter DUE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

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

    @Column(name = "voucher_code", length = 20)
    private String voucherCode;

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

    // null cho đến khi nhận đồ (đơn online)
    @Column(name = "received_at")
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", columnDefinition = "varchar(10) not null default 'OFFLINE'", updatable = false)
    @Builder.Default
    private OrderChannel channel = OrderChannel.OFFLINE;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    // Dữ liệu cũ; đơn theo hình thức giặt để null nên expressFee = 0
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20)
    private OrderType orderType;

    @Column(name = "wash_mode_id")
    private Long washModeId;

    @Column(name = "wash_mode_name", length = 50)
    private String washModeName;

    @Column(name = "wash_mode_hours")
    private Integer washModeHours;

    // serviceId -> đơn giá danh mục chụp lúc tạo đơn
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requested_service_prices", columnDefinition = "jsonb")
    private Map<Long, BigDecimal> requestedServicePrices;

    // serviceId -> express_surcharge của đơn EXPRESS cũ chưa phân loại; chỉ script ghi
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legacy_service_surcharges", columnDefinition = "jsonb")
    private Map<Long, BigDecimal> legacyServiceSurcharges;

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

    // Tổng phần trừ vào đơn của các phiếu sự cố đã duyệt, tách khỏi giảm giá voucher (QĐ4)
    @Column(name = "incident_discount", nullable = false, precision = 12, scale = 0,
            columnDefinition = "numeric(12,0) not null default 0")
    @Builder.Default
    private BigDecimal incidentDiscount = BigDecimal.ZERO;

    // Điều khoản bồi thường chốt lúc tạo đơn; null = đơn tạo trước khi chốt, dùng cấu hình hiện hành
    @Column(name = "compensation_multiplier")
    private Integer compensationMultiplier;

    @Column(name = "compensation_cap", precision = 12, scale = 0)
    private BigDecimal compensationCap;

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

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrderFulfillment fulfillment;

    @Column(name = "payment_claimed_at")
    private Instant paymentClaimedAt;

    @Column(name = "payment_claim_rejected_at")
    private Instant paymentClaimRejectedAt;

    @Column(name = "payment_claim_rejected_reason", length = 200)
    private String paymentClaimRejectedReason;

    /** null/false = voucher do nhân viên áp hoặc đơn cũ. */
    @Column(name = "voucher_by_customer")
    private Boolean voucherByCustomer;

    /** Đơn khách tự thanh toán: ONLINE giao tận nơi, đã hoàn thành, chưa thanh toán (QR, voucher, báo thanh toán). */
    public boolean customerPayable() {
        return this.channel == OrderChannel.ONLINE && this.fulfillment != null
                && this.fulfillment.getReturnMethod() == ReturnMethod.DELIVERY
                && this.status == OrderStatus.COMPLETED && this.paymentStatus == PaymentStatus.UNPAID;
    }

    /** Khách chỉ sửa voucher của chính mình, trước khi báo đã thanh toán (FR-044). */
    public boolean voucherEditableByCustomer() {
        return customerPayable() && this.paymentClaimedAt == null
                && (this.voucherId == null || Boolean.TRUE.equals(this.voucherByCustomer));
    }

    public void requireCustomerPayable() {
        if (customerPayable()) return;
        boolean atPaymentStep = this.channel == OrderChannel.ONLINE && this.fulfillment != null
                && this.fulfillment.getReturnMethod() == ReturnMethod.DELIVERY && this.status == OrderStatus.COMPLETED;
        throw new BusinessException(atPaymentStep ? "Đơn hàng đã được thanh toán" : "Đơn chưa đến bước thanh toán");
    }

    /** Khách báo đã chuyển khoản; chỉ là báo, paymentStatus giữ UNPAID; bấm lại không đổi gì (FR-047). */
    public void claimPayment(String customerCode) {
        requireCustomerPayable();
        if (this.paymentClaimedAt != null) return;
        this.paymentClaimedAt = Instant.now();
        this.paymentClaimRejectedAt = null;
        this.paymentClaimRejectedReason = null;
        addHistory(HistoryActionType.PAYMENT_CLAIM, null, null, null, customerCode);
    }

    /** Nhân viên từ chối báo thanh toán: xoá báo, lưu lý do cho khách, voucher mở lại theo luật (FR-048). */
    public void rejectPaymentClaim(String reason, String staff) {
        if (this.paymentStatus == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng đã được thanh toán");
        }
        if (this.paymentClaimedAt == null) {
            throw new BusinessException("Đơn không có báo thanh toán để từ chối");
        }
        this.paymentClaimedAt = null;
        this.paymentClaimRejectedAt = Instant.now();
        this.paymentClaimRejectedReason = reason;
        addHistory(HistoryActionType.PAYMENT_CLAIM_REJECTED, null, null, reason, staff);
    }


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

    /** Tổng theo QĐ4: Σ thành tiền + phụ phí nhanh + phụ phí đồ trắng − giảm giá voucher − giảm giá sự cố (tổng hai khoản giảm ≤ tiền hàng). */
    public void recalcTotals(BigDecimal requestedDiscount) {
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
        BigDecimal incident = incidentDiscountOrZero();
        BigDecimal discount = requestedDiscount == null ? BigDecimal.ZERO
                : requestedDiscount.min(sub.subtract(incident)).max(BigDecimal.ZERO);
        this.subtotal = sub;
        this.expressFee = express;
        this.discountAmount = discount;
        this.totalAmount = sub.add(express).add(this.whitesFee).subtract(discount).subtract(incident)
                .max(BigDecimal.ZERO);
    }

    public int effectiveMultiplier(int fallback) {
        return this.compensationMultiplier != null ? this.compensationMultiplier : fallback;
    }

    /** Số tiền khách phải trả = tiền dịch vụ + phí lấy + phí giao (FR-042); không lưu. */
    public BigDecimal payableAmount() {
        return fulfillment == null ? totalAmount
                : totalAmount.add(fulfillment.getPickupFee()).add(fulfillment.getDeliveryFee());
    }

    public BigDecimal effectiveCap(BigDecimal fallback) {
        return this.compensationCap != null ? this.compensationCap : fallback;
    }

    private BigDecimal incidentDiscountOrZero() {
        return this.incidentDiscount == null ? BigDecimal.ZERO : this.incidentDiscount;
    }

    public void updateItems(List<OrderItem> newItems, String performedBy) {
        requireReceived();
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
        requireReceived();
        rejectCancelTarget(newStatus);
        if (this.status.isTerminal()) {
            throw new BusinessException("Đơn hàng đang ở trạng thái '" + this.status.getDescription() + "', không thể chuyển tiếp");
        }
        if (this.status == newStatus) {
            throw new BusinessException("Đơn hàng đã ở trạng thái '" + newStatus.getDescription() + "'");
        }
        rejectBeforeDropoffTarget(newStatus);
        checkReturnTransition(newStatus);
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        syncReturnStatus(newStatus, performedBy);
        if (newStatus == OrderStatus.COMPLETED) {
            this.completedAt = Instant.now();
        } else if (newStatus == OrderStatus.DELIVERED) {
            this.deliveredAt = Instant.now();
            this.deliveredBy = performedBy;
        }
        addHistory(HistoryActionType.STATUS_CHANGE, oldStatus.name(), newStatus.name(), reason, performedBy);
    }

    public void complete(String shelfLocation, String performedBy) {
        requireReceived();
        if (this.status == OrderStatus.CANCELLED || this.status == OrderStatus.DELIVERED) {
            throw new BusinessException("Đơn hàng đã kết thúc không thể chuyển sang Hoàn thành");
        }
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.COMPLETED;
        syncReturnStatus(OrderStatus.COMPLETED, performedBy);
        this.shelfLocation = shelfLocation != null ? shelfLocation.trim() : null;
        this.completedAt = Instant.now();
        addHistory(HistoryActionType.STATUS_CHANGE, oldStatus.name(), OrderStatus.COMPLETED.name(),
                "Hoàn thành xử lý, xếp lên kệ: " + (this.shelfLocation != null ? this.shelfLocation : "Chưa gắn kệ"),
                performedBy);
    }

    public void deliver(String receiverName, String receiverRelation, String performedBy) {
        requireReceived();
        if (this.status != OrderStatus.COMPLETED) {
            throw new BusinessException("Đơn hàng phải ở trạng thái 'Hoàn thành / Chờ lấy' mới có thể giao đồ");
        }
        checkReturnTransition(OrderStatus.DELIVERED);
        String name = receiverName != null ? receiverName.trim() : null;
        String note = (name != null && !name.isBlank())
                ? "Giao cho người nhận hộ: " + name + " (" + (receiverRelation != null ? receiverRelation.trim() : null) + ")"
                : "Giao đồ trực tiếp cho khách hàng";
        markDelivered(receiverName, receiverRelation, note, performedBy);
    }

    /** Đơn online giao tận nơi, đang giao, đã thanh toán: khách được bấm "Đã nhận được đơn" (FR-056). */
    public boolean receiptConfirmable() {
        return isOnlineDelivery() && this.status == OrderStatus.COMPLETED
                && this.fulfillment.getReturnStatus() == ReturnStatus.DELIVERING && this.paymentStatus == PaymentStatus.PAID;
    }

    /** Khách xác nhận đã nhận đơn -> DELIVERED; bấm lại hoặc nhân viên đã hoàn tất thì không đổi gì (FR-057, FR-058). */
    public void confirmReceived(String customerCode) {
        if (isOnlineDelivery() && this.status == OrderStatus.DELIVERED) return;
        if (!isOnlineDelivery()) {
            throw new BusinessException("Đơn này không giao tận nơi");
        }
        if (this.status != OrderStatus.COMPLETED || this.fulfillment.getReturnStatus() != ReturnStatus.DELIVERING) {
            throw new BusinessException("Đơn chưa được giao, chưa thể xác nhận đã nhận");
        }
        if (this.paymentStatus != PaymentStatus.PAID) {
            throw new BusinessException("Đơn chưa thanh toán");
        }
        markDelivered(null, null, "Khách xác nhận đã nhận đơn", customerCode);
    }

    private boolean isOnlineDelivery() {
        return this.channel == OrderChannel.ONLINE && this.fulfillment != null
                && this.fulfillment.getReturnMethod() == ReturnMethod.DELIVERY;
    }

    private void markDelivered(String receiverName, String receiverRelation, String note, String performedBy) {
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.DELIVERED;
        syncReturnStatus(OrderStatus.DELIVERED, performedBy);
        this.deliveredAt = Instant.now();
        this.deliveredBy = performedBy;
        this.receiverName = receiverName != null ? receiverName.trim() : null;
        this.receiverRelation = receiverRelation != null ? receiverRelation.trim() : null;
        addHistory(HistoryActionType.STATUS_CHANGE, oldStatus.name(), OrderStatus.DELIVERED.name(), note, performedBy);
    }

    public void revertStatus(OrderStatus targetStatus, String reason, String performedBy) {
        requireReceived();
        rejectCancelTarget(targetStatus);
        if (this.status.isTerminal()) {
            throw new BusinessException("Không thể lùi trạng thái của đơn hàng đã kết thúc");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do khi lùi trạng thái đơn hàng (BR-D06)");
        }
        if (this.status == targetStatus) {
            throw new BusinessException("Đơn hàng đã ở trạng thái '" + targetStatus.getDescription() + "'");
        }
        rejectBeforeDropoffTarget(targetStatus);
        checkReturnTransition(targetStatus);
        OrderStatus oldStatus = this.status;
        this.status = targetStatus;
        syncReturnStatus(targetStatus, performedBy);
        if (targetStatus != OrderStatus.COMPLETED) {
            this.completedAt = null;
        }
        addHistory(HistoryActionType.REVERT_STATUS, oldStatus.name(), targetStatus.name(), reason, performedBy);
    }

    public void reschedule(Instant newDueAt, String reason, String performedBy) {
        requireReceived();
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

    /**
     * Áp / đổi / gỡ voucher ở bước thanh toán: thay tiền giảm cũ bằng tiền giảm mới, giữ các điều chỉnh giá khác.
     * Còn 0đ thì không có gì để thu nên coi như đã thanh toán; gỡ voucher thì trở lại chưa thanh toán.
     */
    public void applyVoucher(Long voucherId, String code, BigDecimal discount, String performedBy) {
        requireReceived();
        if (this.status != OrderStatus.COMPLETED) {
            throw new BusinessException("Chỉ áp voucher khi thanh toán đơn đã hoàn thành");
        }
        BigDecimal oldDiscount = this.discountAmount == null ? BigDecimal.ZERO : this.discountAmount;
        boolean paidByZero = this.paymentStatus == PaymentStatus.PAID && payableAmount().signum() == 0 && oldDiscount.signum() > 0;
        if (this.paymentStatus == PaymentStatus.PAID && !paidByZero) {
            throw new BusinessException("Đơn đã thanh toán, không áp voucher được nữa");
        }
        if (discount != null) {
            BigDecimal room = this.subtotal.subtract(incidentDiscountOrZero()).max(BigDecimal.ZERO);
            discount = discount.min(room);
        }
        String oldCode = this.voucherCode;
        this.voucherId = voucherId;
        this.voucherCode = code;
        this.discountAmount = discount;
        this.totalAmount = this.totalAmount.add(oldDiscount).subtract(discount).max(BigDecimal.ZERO);
        this.paymentStatus = payableAmount().signum() == 0 ? PaymentStatus.PAID : PaymentStatus.UNPAID;
        addHistory(HistoryActionType.PRICE_ADJUST, oldCode, code,
                code == null ? "Gỡ voucher" : "Áp voucher " + code + " giảm " + discount.toPlainString() + "đ", performedBy);
    }

    /**
     * Trừ số tiền phiếu sự cố đã duyệt vào đơn chưa thanh toán (BR-I07); trả phần đã trừ, phần còn lại phải chi cho khách.
     * Còn 0đ thì không có gì để thu nên coi như đã thanh toán, giống voucher.
     */
    public BigDecimal applyIncidentDiscount(BigDecimal amount, String incidentCode, String performedBy) {
        if (this.paymentStatus == PaymentStatus.PAID || amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal voucher = this.discountAmount == null ? BigDecimal.ZERO : this.discountAmount;
        BigDecimal room = this.subtotal.subtract(voucher).subtract(incidentDiscountOrZero()).max(BigDecimal.ZERO);
        BigDecimal deducted = amount.min(room);
        if (deducted.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal oldTotal = this.totalAmount;
        this.incidentDiscount = incidentDiscountOrZero().add(deducted);
        this.totalAmount = this.totalAmount.subtract(deducted).max(BigDecimal.ZERO);
        if (payableAmount().signum() == 0) {
            this.paymentStatus = PaymentStatus.PAID;
        }
        addHistory(HistoryActionType.PRICE_ADJUST, String.valueOf(oldTotal), String.valueOf(this.totalAmount),
                "Giảm giá sự cố " + incidentCode + " " + deducted.toPlainString() + "đ", performedBy);
        return deducted;
    }

    public void adjustPrice(BigDecimal newAmount, String reason, String performedBy) {
        requireReceived();
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

    /** Chỉ cancel / reject vào được CANCELLED, và chỉ khi đơn chưa nhận đồ (FR-032). */
    public void cancel(String customerReason, String reason, String performedBy) {
        if (!this.status.isBeforeDropoff()) {
            throw new BusinessException("Chỉ hủy được đơn chưa nhận đồ");
        }
        String historyReason = reason;
        if (this.channel == OrderChannel.ONLINE) {
            if (customerReason == null || customerReason.isBlank()) {
                throw new BusinessException("Bắt buộc phải nhập lý do hủy gửi khách");
            }
            if (historyReason == null || historyReason.isBlank()) {
                historyReason = "Hủy đơn online";
            }
        } else if (reason == null || reason.isBlank()) {
            throw new BusinessException("Bắt buộc phải nhập lý do hủy đơn hàng (BR-D05)");
        }
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.CANCELLED;
        if (this.fulfillment != null) {
            if (this.channel == OrderChannel.ONLINE) {
                this.fulfillment.setCustomerRejectReason(customerReason.trim());
            }
            DropoffStatus oldDropoff = this.fulfillment.getDropoffStatus();
            if (oldDropoff != null) {
                this.fulfillment.setDropoffStatus(null);
                addHistory(HistoryActionType.DROPOFF_STATUS_CHANGE, oldDropoff.name(), null, null, performedBy);
            }
        }
        addHistory(HistoryActionType.CANCELLED, oldStatus.name(), OrderStatus.CANCELLED.name(), historyReason, performedBy);
    }

    public void confirm(String performedBy) {
        requirePendingOnline();
        this.status = OrderStatus.AWAITING_DROPOFF;
        this.confirmedAt = Instant.now();
        this.confirmedBy = performedBy;
        this.fulfillment.setDropoffStatus(this.fulfillment.getDropoffMethod() == DropoffMethod.PICKUP
                ? DropoffStatus.AWAITING_PICKUP : DropoffStatus.AWAITING_CUSTOMER_DROPOFF);
        addHistory(HistoryActionType.CONFIRMED, OrderStatus.PENDING_CONFIRMATION.name(),
                OrderStatus.AWAITING_DROPOFF.name(), null, performedBy);
    }

    /** Từ chối = hủy từ Chờ xác nhận; lý do nội bộ chỉ vào lịch sử (R14). */
    public void reject(String customerReason, String reason, String performedBy) {
        requirePendingOnline();
        cancel(customerReason, reason == null || reason.isBlank() ? "Từ chối đơn online" : reason.trim(), performedBy);
    }

    /** Giờ hẹn đã được service kiểm bằng {@link AppointmentRules} (cần setting giờ mở cửa); null = giữ nguyên. */
    public void rescheduleAppointment(Instant appointmentAt, Instant returnAt, String performedBy) {
        if (!this.status.isBeforeDropoff() || this.fulfillment == null) {
            throw new BusinessException("Chỉ đổi giờ hẹn khi đơn chưa nhận đồ");
        }
        if (appointmentAt == null && returnAt == null) {
            throw new BusinessException("Chọn giờ hẹn mới");
        }
        if (appointmentAt != null && !appointmentAt.equals(this.fulfillment.getAppointmentAt())) {
            addHistory(HistoryActionType.RESCHEDULE, String.valueOf(this.fulfillment.getAppointmentAt()),
                    String.valueOf(appointmentAt), "Giờ hẹn gửi", performedBy);
            this.fulfillment.setAppointmentAt(appointmentAt);
        }
        if (returnAt != null && !returnAt.equals(this.dueAt)) {
            addHistory(HistoryActionType.RESCHEDULE, String.valueOf(this.dueAt), String.valueOf(returnAt),
                    "Giờ hẹn nhận", performedBy);
            this.dueAt = returnAt;
        }
    }

    /**
     * Đồ về tiệm (khách tự mang tới hoặc tiệm đi lấy): mở khóa xử lý và chốt dueAt theo
     * {@link #resolveDropoffDue} (FR-014, FR-039); requestedDueAt null = giữ hạn gợi ý.
     */
    public void completeDropoff(String pickupTrackingCode, Instant now, Instant requestedDueAt, LocalTime open,
                                LocalTime close, String performedBy) {
        if (this.status != OrderStatus.AWAITING_DROPOFF || this.fulfillment == null) {
            throw new BusinessException("Đơn đã được xử lý, vui lòng tải lại");
        }
        boolean pickup = this.fulfillment.getDropoffMethod() == DropoffMethod.PICKUP;
        resolveDropoffDue(now, requestedDueAt, open, close, performedBy);
        changePickupCode(pickupTrackingCode, performedBy);
        DropoffStatus oldDropoff = this.fulfillment.getDropoffStatus();
        DropoffStatus newDropoff = pickup ? DropoffStatus.PICKED_UP : DropoffStatus.CUSTOMER_DROPPED_OFF;
        this.fulfillment.setDropoffStatus(newDropoff);
        this.receivedAt = now;
        this.status = OrderStatus.RECEIVED;
        addHistory(HistoryActionType.DROPOFF_STATUS_CHANGE, oldDropoff == null ? null : oldDropoff.name(),
                newDropoff.name(), null, performedBy);
        addHistory(HistoryActionType.STATUS_CHANGE, OrderStatus.AWAITING_DROPOFF.name(), OrderStatus.RECEIVED.name(),
                null, performedBy);
    }

    /**
     * Chỉ đổi được khi trễ giờ hẹn và dueAt hiện tại sớm hơn minDue = dueAtFor(now, washModeHours): requested null
     * → minDue; sớm hơn minDue → lỗi field dueAt; khác dueAt cũ → ghi RESCHEDULE. Mọi ca khác (đúng hẹn, trễ mà vẫn
     * kịp) giữ dueAt; requested khác dueAt → lỗi field dueAt (FR-039).
     */
    private void resolveDropoffDue(Instant now, Instant requestedDueAt, LocalTime open, LocalTime close,
                                   String performedBy) {
        Instant appt = this.fulfillment.getAppointmentAt();
        Instant minDue = appt != null && now.isAfter(appt) && this.washModeHours != null && this.dueAt != null
                ? AppointmentRules.dueAtFor(now, this.washModeHours, open, close) : null;
        if (minDue == null || !minDue.isAfter(this.dueAt)) {
            if (requestedDueAt != null && !requestedDueAt.equals(this.dueAt)) {
                throw new BusinessException("Hạn trả hiện tại vẫn kịp nên không đổi được", "dueAt");
            }
            return;
        }
        Instant newDue = requestedDueAt != null ? requestedDueAt : minDue;
        if (newDue.isBefore(minDue)) {
            throw new BusinessException("Hạn trả phải từ " + DUE_FORMAT.format(minDue) + " trở đi", "dueAt");
        }
        boolean lateDefault = newDue.equals(minDue);
        String note = !lateDefault ? "Đổi hạn trả khi nhận đồ"
                : this.fulfillment.getDropoffMethod() == DropoffMethod.PICKUP
                        ? "Tiệm tới nhận trễ giờ hẹn" : "Khách tới trễ giờ hẹn";
        addHistory(HistoryActionType.RESCHEDULE, String.valueOf(this.dueAt), String.valueOf(newDue), note,
                performedBy);
        this.dueAt = newDue;
    }

    public void startDelivery(String deliveryTrackingCode, String performedBy) {
        if (this.fulfillment == null || this.fulfillment.getReturnMethod() != ReturnMethod.DELIVERY) {
            throw new BusinessException("Đơn không có chặng giao đồ");
        }
        if (this.status != OrderStatus.COMPLETED) {
            throw new BusinessException("Đơn chưa hoàn thành");
        }
        if (this.fulfillment.getReturnStatus() != ReturnStatus.AWAITING_DELIVERY) {
            throw new BusinessException("Đơn đã được xử lý, vui lòng tải lại");
        }
        if (this.paymentStatus != PaymentStatus.PAID) {
            throw new BusinessException("Đơn chưa thanh toán, chưa thể bắt đầu giao");
        }
        changeDeliveryCode(deliveryTrackingCode, performedBy);
        setReturnStatus(ReturnStatus.DELIVERING, null, performedBy);
    }

    /** Hoàn tác duy nhất của chiều trả: Đang giao → Chờ giao. */
    public void undoStartDelivery(String reason, String performedBy) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("Nhập lý do");
        }
        if (trimmed.length() > 500) {
            throw new BusinessException("Lý do tối đa 500 ký tự");
        }
        if (this.fulfillment == null || this.status != OrderStatus.COMPLETED
                || this.fulfillment.getReturnStatus() != ReturnStatus.DELIVERING) {
            throw new BusinessException("Đơn đã được xử lý, vui lòng tải lại");
        }
        setReturnStatus(ReturnStatus.AWAITING_DELIVERY, trimmed, performedBy);
    }

    /** null = giữ nguyên, rỗng = xóa. */
    public void updateTrackingCodes(String pickupTrackingCode, String deliveryTrackingCode, String performedBy) {
        if (this.fulfillment == null || this.status.isTerminal()) {
            throw new BusinessException("Chỉ sửa mã vận đơn của đơn online chưa kết thúc");
        }
        // kiểm cả hai mã trước khi ghi để không đổi một nửa
        normalizeTrackingCode(pickupTrackingCode,
                this.fulfillment.getDropoffMethod() == DropoffMethod.PICKUP, "Đơn không có chặng lấy đồ");
        normalizeTrackingCode(deliveryTrackingCode,
                this.fulfillment.getReturnMethod() == ReturnMethod.DELIVERY, "Đơn không có chặng giao đồ");
        changePickupCode(pickupTrackingCode, performedBy);
        changeDeliveryCode(deliveryTrackingCode, performedBy);
    }

    private void changePickupCode(String raw, String performedBy) {
        if (raw == null) {
            return;
        }
        String code = normalizeTrackingCode(raw,
                this.fulfillment.getDropoffMethod() == DropoffMethod.PICKUP, "Đơn không có chặng lấy đồ");
        String old = this.fulfillment.getPickupTrackingCode();
        if (!java.util.Objects.equals(old, code)) {
            this.fulfillment.setPickupTrackingCode(code);
            addHistory(HistoryActionType.TRACKING_CODE_CHANGE, old, code, "Chặng lấy", performedBy);
        }
    }

    private void changeDeliveryCode(String raw, String performedBy) {
        if (raw == null) {
            return;
        }
        String code = normalizeTrackingCode(raw,
                this.fulfillment.getReturnMethod() == ReturnMethod.DELIVERY, "Đơn không có chặng giao đồ");
        String old = this.fulfillment.getDeliveryTrackingCode();
        if (!java.util.Objects.equals(old, code)) {
            this.fulfillment.setDeliveryTrackingCode(code);
            addHistory(HistoryActionType.TRACKING_CODE_CHANGE, old, code, "Chặng giao", performedBy);
        }
    }

    /** Cắt khoảng trắng, rỗng → null; kiểm độ dài và chặng (FR-037). */
    private static String normalizeTrackingCode(String raw, boolean legExists, String noLegMessage) {
        String code = raw == null || raw.isBlank() ? null : raw.trim();
        if (code == null) {
            return null;
        }
        if (code.length() > 50) {
            throw new BusinessException("Mã vận đơn tối đa 50 ký tự");
        }
        if (!legExists) {
            throw new BusinessException(noLegMessage);
        }
        return code;
    }

    public static final String RETURN_NOT_READY_MESSAGE = "Đơn giao tận nơi phải chuyển sang Đang giao trước";

    /** Kiểm trước khi đổi status: vào DELIVERED đòi bước cuối chiều trả sẵn sàng; rời COMPLETED bị chặn khi Đang giao. */
    private void checkReturnTransition(OrderStatus target) {
        if (this.fulfillment == null || this.status == target) {
            return;
        }
        ReturnStatus current = this.fulfillment.getReturnStatus();
        if (target == OrderStatus.DELIVERED) {
            ReturnStatus ready = this.fulfillment.getReturnMethod() == ReturnMethod.DELIVERY
                    ? ReturnStatus.DELIVERING : ReturnStatus.AWAITING_CUSTOMER_COLLECT;
            if (current != ready) {
                throw new BusinessException(RETURN_NOT_READY_MESSAGE);
            }
        } else if (current == ReturnStatus.DELIVERING) {
            throw new BusinessException("Đơn đang giao, hãy hoàn tác Đang giao trước");
        }
    }

    /** Đồng bộ return_status theo status mới (gọi sau khi đổi status). */
    private void syncReturnStatus(OrderStatus target, String performedBy) {
        if (this.fulfillment == null) {
            return;
        }
        boolean delivery = this.fulfillment.getReturnMethod() == ReturnMethod.DELIVERY;
        ReturnStatus current = this.fulfillment.getReturnStatus();
        ReturnStatus next = switch (target) {
            case COMPLETED -> current != null ? current
                    : delivery ? ReturnStatus.AWAITING_DELIVERY : ReturnStatus.AWAITING_CUSTOMER_COLLECT;
            case DELIVERED -> delivery ? ReturnStatus.DELIVERED : ReturnStatus.CUSTOMER_COLLECTED;
            default -> null;
        };
        setReturnStatus(next, null, performedBy);
    }

    private void setReturnStatus(ReturnStatus next, String reason, String performedBy) {
        ReturnStatus old = this.fulfillment.getReturnStatus();
        if (old == next) {
            return;
        }
        this.fulfillment.setReturnStatus(next);
        addHistory(HistoryActionType.RETURN_STATUS_CHANGE, old == null ? null : old.name(),
                next == null ? null : next.name(), reason, performedBy);
    }

    private static void rejectBeforeDropoffTarget(OrderStatus target) {
        if (target.isBeforeDropoff()) {
            throw new BusinessException("Không thể chuyển về trạng thái trước khi nhận đồ");
        }
    }

    private void requirePendingOnline() {
        if (this.channel != OrderChannel.ONLINE || this.status != OrderStatus.PENDING_CONFIRMATION
                || this.fulfillment == null) {
            throw new BusinessException("Đơn đã được xử lý, vui lòng tải lại");
        }
    }

    public static final String BEFORE_DROPOFF_MESSAGE ="Đơn online chưa nhận đồ, chưa thể thực hiện thao tác này";

    private void requireReceived() {
        if (this.status.isBeforeDropoff()) {
            throw new BusinessException(BEFORE_DROPOFF_MESSAGE);
        }
    }

    private static void rejectCancelTarget(OrderStatus target) {
        if (target == OrderStatus.CANCELLED) {
            throw new BusinessException("Dùng thao tác Hủy đơn để hủy");
        }
    }

    public void markReceiptPrinted() {
        if (this.receiptPrintedAt == null) {
            this.receiptPrintedAt = Instant.now();
        }
    }

    /** Dịch vụ phát sinh: đơn có dịch vụ khách chọn nhưng dòng dùng dịch vụ ngoài danh sách đó (FR-009, FR-012). */
    public boolean isExtraService(Long serviceId) {
        return requestedServiceIds != null && !requestedServiceIds.isEmpty()
                && serviceId != null && !requestedServiceIds.contains(serviceId);
    }
}
