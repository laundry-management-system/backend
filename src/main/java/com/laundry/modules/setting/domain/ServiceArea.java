package com.laundry.modules.setting.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Khu vực giao nhận và phí mỗi lượt; không xoá, chỉ ngừng phục vụ (FR-029d). Unique lower(name) nằm trong data.sql. */
@Entity
@Table(name = "service_areas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceArea extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "pickup_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal pickupFee;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal deliveryFee;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    public ServiceArea(String name, BigDecimal pickupFee, BigDecimal deliveryFee) {
        update(name, pickupFee, deliveryFee);
    }

    public void update(String name, BigDecimal pickupFee, BigDecimal deliveryFee) {
        this.name = name;
        this.pickupFee = pickupFee;
        this.deliveryFee = deliveryFee;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
