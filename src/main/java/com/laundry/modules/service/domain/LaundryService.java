package com.laundry.modules.service.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "services",
        indexes = {
                @Index(name = "idx_services_code", columnList = "service_code"),
                @Index(name = "idx_services_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LaundryService extends BaseEntity {

    @Column(name = "service_code", nullable = false, unique = true, length = 10)
    private String serviceCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private ServiceUnit unit;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal currentPrice;

    @Column(name = "standard_hours", nullable = false)
    private Integer standardHours;

    @Column(name = "express_supported", nullable = false)
    @Builder.Default
    private Boolean expressSupported = false;

    @Column(name = "express_surcharge", precision = 12, scale = 0)
    private BigDecimal expressSurcharge;

    @Column(name = "express_hours")
    private Integer expressHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    public void validateBusinessRules() {
        if (this.currentPrice == null || this.currentPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Service price must be greater than or equal to 0 (BR-C01)");
        }
        if (this.standardHours == null || this.standardHours <= 0) {
            throw new BusinessException("Standard processing time must be greater than 0 hours (BR-C06)");
        }
        if (Boolean.TRUE.equals(this.expressSupported)) {
            if (this.expressSurcharge == null || this.expressSurcharge.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Express surcharge must be greater than or equal to 0");
            }
            if (this.expressHours == null || this.expressHours <= 0) {
                throw new BusinessException("Express processing time must be greater than 0 hours");
            }
            if (this.expressHours >= this.standardHours) {
                throw new BusinessException("Express processing time (" + this.expressHours + "h) must be shorter than standard processing time (" + this.standardHours + "h) (BR-C06)");
            }
        }
    }

    public void updateDetails(
            String name,
            ServiceUnit unit,
            BigDecimal currentPrice,
            Integer standardHours,
            Boolean expressSupported,
            BigDecimal expressSurcharge,
            Integer expressHours
    ) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
        if (unit != null) {
            this.unit = unit;
        }
        if (currentPrice != null) {
            this.currentPrice = currentPrice;
        }
        if (standardHours != null) {
            this.standardHours = standardHours;
        }
        if (expressSupported != null) {
            this.expressSupported = expressSupported;
        }
        this.expressSurcharge = expressSurcharge;
        this.expressHours = expressHours;
        validateBusinessRules();
    }

    public void deactivate() {
        if (this.status == ServiceStatus.INACTIVE) {
            throw new BusinessException("Service is already inactive");
        }
        this.status = ServiceStatus.INACTIVE;
    }

    public void activate() {
        if (this.status == ServiceStatus.ACTIVE) {
            throw new BusinessException("Service is already active");
        }
        this.status = ServiceStatus.ACTIVE;
    }
}
