package com.laundry.modules.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);

    List<Order> findByStatus(OrderStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);

    @Query("""
        SELECT o FROM Order o
        WHERE (:status IS NULL OR o.status = :status)
          AND (:priority IS NULL OR o.priority = :priority)
          AND (:phone IS NULL OR o.customerPhone LIKE CONCAT('%', CAST(:phone AS string), '%'))
          AND (:search IS NULL OR LOWER(o.orderCode) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%') 
                                OR LOWER(o.customerName) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """)
    Page<Order> searchOrders(
            @Param("status") OrderStatus status,
            @Param("priority") OrderPriority priority,
            @Param("phone") String phone,
            @Param("search") String search,
            Pageable pageable
    );
}

