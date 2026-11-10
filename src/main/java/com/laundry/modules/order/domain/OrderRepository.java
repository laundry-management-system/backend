package com.laundry.modules.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByStatus(OrderStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);

    List<Order> findByIntakeRequestId(java.util.UUID intakeRequestId);

    List<Order> findByIntakeCode(String intakeCode);

    /** [status, count] cho thanh đếm theo trạng thái. */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();

    /** Đơn v3 đang Mới nhận mà chưa phân loại (chưa có dòng đồ). */
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.status = com.laundry.modules.order.domain.OrderStatus.RECEIVED
          AND o.intakeCode IS NOT NULL AND o.items IS EMPTY
    """)
    long countUnclassified();

    /** Đơn chưa hoàn thành mà đã quá ngày hẹn. */
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.status IN (com.laundry.modules.order.domain.OrderStatus.RECEIVED,
                           com.laundry.modules.order.domain.OrderStatus.PROCESSING)
          AND o.dueAt < :now
    """)
    long countLate(@Param("now") Instant now);
}
