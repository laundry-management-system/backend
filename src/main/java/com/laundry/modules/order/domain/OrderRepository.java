package com.laundry.modules.order.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Mặc định danh sách đơn: đơn chưa nhận đồ (receivedAt NULL) lên đầu theo ngày đặt mới nhất. */
    Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("receivedAt").nullsFirst(), Sort.Order.desc("createdAt"));

    @EntityGraph(attributePaths = "fulfillment")
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    /** Giới hạn đơn chưa kết thúc mỗi khách (FR-007). */
    long countByCustomerIdAndChannelAndStatusNotIn(Long customerId, OrderChannel channel, List<OrderStatus> statuses);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndCustomerId(String orderCode, Long customerId);

    /** Khóa đơn khi áp voucher để không chạy chồng với xác nhận thanh toán */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "fulfillment")
    List<Order> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);

    List<Order> findByStatus(OrderStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);

    List<Order> findByIntakeRequestId(java.util.UUID intakeRequestId);

    List<Order> findByIntakeCode(String intakeCode);

    /** {@code statuses} không được rỗng: không lọc thì truyền mọi trạng thái. */
    @Query("""
        SELECT o FROM Order o
        WHERE (:status IS NULL OR o.status = :status)
          AND o.status IN :statuses
          AND (:channel IS NULL OR o.channel = :channel)
          AND (:priority IS NULL OR o.priority = :priority)
          AND (:washModeId IS NULL OR o.washModeId = :washModeId)
          AND (:classified IS NULL OR (:classified = true AND SIZE(o.items) > 0) OR (:classified = false AND SIZE(o.items) = 0))
          AND (:late IS NULL OR :late = false
               OR (o.dueAt < :now AND o.status IN (com.laundry.modules.order.domain.OrderStatus.RECEIVED,
                                                   com.laundry.modules.order.domain.OrderStatus.PROCESSING)))
          AND (CAST(:dueStart AS instant) IS NULL OR o.dueAt >= :dueStart)
          AND (CAST(:dueEnd AS instant) IS NULL OR o.dueAt < :dueEnd)
          AND (:phone IS NULL OR o.customerPhone LIKE CONCAT('%', CAST(:phone AS string), '%'))
          AND (:intakeCode IS NULL OR o.intakeCode = :intakeCode)
          AND (:search IS NULL OR LOWER(o.orderCode) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                                OR LOWER(o.customerName) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """)
    @EntityGraph(attributePaths = "fulfillment")
    Page<Order> searchOrders(
            @Param("status") OrderStatus status,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("channel") OrderChannel channel,
            @Param("priority") OrderPriority priority,
            @Param("washModeId") Long washModeId,
            @Param("classified") Boolean classified,
            @Param("late") Boolean late,
            @Param("now") Instant now,
            @Param("dueStart") Instant dueStart,
            @Param("dueEnd") Instant dueEnd,
            @Param("phone") String phone,
            @Param("search") String search,
            @Param("intakeCode") String intakeCode,
            Pageable pageable
    );

    /** Đơn đang mở theo hình thức giặt, gồm cả hình thức đã ngừng; bỏ đơn cũ chưa có hình thức. */
    @Query("""
        SELECT o.washModeId, COUNT(o) FROM Order o
        WHERE o.status IN (com.laundry.modules.order.domain.OrderStatus.RECEIVED, com.laundry.modules.order.domain.OrderStatus.PROCESSING)
          AND o.washModeId IS NOT NULL
          AND (:channel IS NULL OR o.channel = :channel)
          AND (CAST(:dueStart AS instant) IS NULL OR o.dueAt >= :dueStart)
          AND (CAST(:dueEnd AS instant) IS NULL OR o.dueAt < :dueEnd)
          AND (:phone IS NULL OR o.customerPhone LIKE CONCAT('%', CAST(:phone AS string), '%'))
          AND (:search IS NULL OR LOWER(o.orderCode) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                                OR LOWER(o.customerName) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
        GROUP BY o.washModeId
    """)
    List<Object[]> countOpenByMode(
            @Param("channel") OrderChannel channel,
            @Param("dueStart") Instant dueStart,
            @Param("dueEnd") Instant dueEnd,
            @Param("phone") String phone,
            @Param("search") String search
    );

    /** Hai trạng thái trước nhận đồ chỉ vào bộ đếm riêng, không vào các tab còn lại (FR-016). */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN o.status IN (com.laundry.modules.order.domain.OrderStatus.RECEIVED, com.laundry.modules.order.domain.OrderStatus.PROCESSING) THEN 1 ELSE 0 END), 0) AS total,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.RECEIVED
                                  AND SIZE(o.items) = 0 THEN 1 ELSE 0 END), 0) AS unclassified,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.RECEIVED
                                 THEN 1 ELSE 0 END), 0) AS received,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.PROCESSING
                                 THEN 1 ELSE 0 END), 0) AS processing,
               COALESCE(SUM(CASE WHEN o.status IN (com.laundry.modules.order.domain.OrderStatus.RECEIVED, com.laundry.modules.order.domain.OrderStatus.PROCESSING)
                                  AND o.dueAt < :now THEN 1 ELSE 0 END), 0) AS late,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.PENDING_CONFIRMATION
                                 THEN 1 ELSE 0 END), 0) AS pendingConfirmation,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.AWAITING_DROPOFF
                                 THEN 1 ELSE 0 END), 0) AS awaitingDropoff
        FROM Order o
        WHERE o.status IN (com.laundry.modules.order.domain.OrderStatus.PENDING_CONFIRMATION, com.laundry.modules.order.domain.OrderStatus.AWAITING_DROPOFF, com.laundry.modules.order.domain.OrderStatus.RECEIVED, com.laundry.modules.order.domain.OrderStatus.PROCESSING)
          AND (:channel IS NULL OR o.channel = :channel)
          AND (CAST(:dueStart AS instant) IS NULL OR o.dueAt >= :dueStart)
          AND (CAST(:dueEnd AS instant) IS NULL OR o.dueAt < :dueEnd)
          AND (:phone IS NULL OR o.customerPhone LIKE CONCAT('%', CAST(:phone AS string), '%'))
          AND (:search IS NULL OR LOWER(o.orderCode) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                                OR LOWER(o.customerName) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """)
    WorkCounts countWork(
            @Param("now") Instant now,
            @Param("channel") OrderChannel channel,
            @Param("dueStart") Instant dueStart,
            @Param("dueEnd") Instant dueEnd,
            @Param("phone") String phone,
            @Param("search") String search
    );
}
