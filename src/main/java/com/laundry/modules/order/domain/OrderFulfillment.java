package com.laundry.modules.order.domain;

import com.laundry.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_fulfillments")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderFulfillment extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "dropoff_method", nullable = false, length = 20)
    private DropoffMethod dropoffMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_method", nullable = false, length = 20)
    private ReturnMethod returnMethod;

    @Column(name = "pickup_area_id")
    private Long pickupAreaId;

    @Column(name = "pickup_area_name", length = 100)
    private String pickupAreaName;

    @Column(name = "delivery_area_id")
    private Long deliveryAreaId;

    @Column(name = "delivery_area_name", length = 100)
    private String deliveryAreaName;

    @Column(name = "pickup_address", length = 255)
    private String pickupAddress;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(name = "appointment_at", nullable = false)
    private Instant appointmentAt;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "pickup_fee", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal pickupFee = BigDecimal.ZERO;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "dropoff_status", length = 30)
    private DropoffStatus dropoffStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", length = 30)
    private ReturnStatus returnStatus;

    @Column(name = "pickup_tracking_code", length = 50)
    private String pickupTrackingCode;

    @Column(name = "delivery_tracking_code", length = 50)
    private String deliveryTrackingCode;

    @Column(name = "customer_reject_reason", length = 500)
    private String customerRejectReason;
}
