package com.laundry.modules.order.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderImageRepository extends JpaRepository<OrderImage, Long> {

    List<OrderImage> findByOrderItemId(Long orderItemId);
}

