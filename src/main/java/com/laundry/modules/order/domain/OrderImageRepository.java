package com.laundry.modules.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderImageRepository extends JpaRepository<OrderImage, Long> {

    List<OrderImage> findByOrderItemId(Long orderItemId);
}

