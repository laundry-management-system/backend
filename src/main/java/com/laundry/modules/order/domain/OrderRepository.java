package com.laundry.modules.order.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Optional<Order> findByOrderCode(String orderCode);

    /** Khóa đơn khi áp voucher để không chạy chồng với xác nhận thanh toán */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

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
          AND (:priority IS NULL OR o.priority = :priority)
          AND (:orderType IS NULL OR o.orderType = :orderType)
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
    Page<Order> searchOrders(
            @Param("status") OrderStatus status,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("priority") OrderPriority priority,
            @Param("orderType") OrderType orderType,
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

    @Query("""
        SELECT COUNT(o) AS total,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.RECEIVED
                                  AND SIZE(o.items) = 0 THEN 1 ELSE 0 END), 0) AS unclassified,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.RECEIVED
                                 THEN 1 ELSE 0 END), 0) AS received,
               COALESCE(SUM(CASE WHEN o.status = com.laundry.modules.order.domain.OrderStatus.PROCESSING
                                 THEN 1 ELSE 0 END), 0) AS processing,
               COALESCE(SUM(CASE WHEN o.dueAt < :now THEN 1 ELSE 0 END), 0) AS late,
               COALESCE(SUM(CASE WHEN o.orderType = com.laundry.modules.order.domain.OrderType.EXPRESS
                                 THEN 1 ELSE 0 END), 0) AS express
        FROM Order o
        WHERE o.status IN (com.laundry.modules.order.domain.OrderStatus.RECEIVED,
                           com.laundry.modules.order.domain.OrderStatus.PROCESSING)
          AND (CAST(:dueStart AS instant) IS NULL OR o.dueAt >= :dueStart)
          AND (CAST(:dueEnd AS instant) IS NULL OR o.dueAt < :dueEnd)
          AND (:phone IS NULL OR o.customerPhone LIKE CONCAT('%', CAST(:phone AS string), '%'))
          AND (:search IS NULL OR LOWER(o.orderCode) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                                OR LOWER(o.customerName) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """)
    WorkCounts countWork(
            @Param("now") Instant now,
            @Param("dueStart") Instant dueStart,
            @Param("dueEnd") Instant dueEnd,
            @Param("phone") String phone,
            @Param("search") String search
    );
}
