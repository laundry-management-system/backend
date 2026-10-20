package com.laundry.modules.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findByIdAndOrderId(Long id, Long orderId);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
