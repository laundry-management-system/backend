package com.laundry.modules.order.domain;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/** Bộ lọc danh sách đơn (WF-D01 / B02). Mỗi spec trả null khi không lọc. */
public final class OrderSpecs {

    private OrderSpecs() {
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasPriority(OrderPriority priority) {
        return priority == null ? null : (root, q, cb) -> cb.equal(root.get("priority"), priority);
    }

    /** So khớp chính xác: dùng để lấy đơn của đúng một khách. Tìm gần đúng theo SĐT dùng {@link #search}. */
    public static Specification<Order> hasPhone(String phone) {
        return phone == null ? null : (root, q, cb) -> cb.equal(root.get("customerPhone"), phone);
    }

    public static Specification<Order> hasCustomer(Long customerId) {
        return customerId == null ? null : (root, q, cb) -> cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Order> hasIntakeCode(String intakeCode) {
        return intakeCode == null ? null : (root, q, cb) -> cb.equal(root.get("intakeCode"), intakeCode);
    }

    public static Specification<Order> hasPaymentStatus(PaymentStatus paymentStatus) {
        return paymentStatus == null ? null : (root, q, cb) -> cb.equal(root.get("paymentStatus"), paymentStatus);
    }

    public static Specification<Order> hasOrderType(OrderType orderType) {
        return orderType == null ? null : (root, q, cb) -> cb.equal(root.get("orderType"), orderType);
    }

    public static Specification<Order> receivedFrom(Instant from) {
        return from == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("receivedAt"), from);
    }

    public static Specification<Order> receivedBefore(Instant to) {
        return to == null ? null : (root, q, cb) -> cb.lessThan(root.get("receivedAt"), to);
    }

    /** Đã phân loại = đơn trước v3 (đã có giá) hoặc đơn v3 đã có dòng đồ. */
    public static Specification<Order> isClassified(Boolean classified) {
        if (classified == null) return null;
        return classified
                ? (root, q, cb) -> cb.or(cb.isNull(root.get("intakeCode")), cb.isNotEmpty(root.get("items")))
                : (root, q, cb) -> cb.and(cb.isNotNull(root.get("intakeCode")), cb.isEmpty(root.get("items")));
    }

    /** Tìm gần đúng theo mã đơn, tên khách hoặc SĐT. */
    public static Specification<Order> search(String term) {
        if (term == null) return null;
        String like = "%" + term.toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("orderCode")), like),
                cb.like(cb.lower(root.get("customerName")), like),
                cb.like(root.get("customerPhone"), "%" + term + "%"));
    }
}
