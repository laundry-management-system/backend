package com.laundry.modules.service.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@NoArgsConstructor
@AllArgsConstructor
public class LaundryService extends BaseEntity {

    @Column(name = "service_code", nullable = false, unique = true, length = 10)
    private String serviceCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private ServiceUnit unit;

    // Chỉ hiển thị ở quầy; chốt vào dòng đồ lúc phân loại
    @Column(name = "min_quantity", nullable = false, precision = 10, scale = 2, columnDefinition = "numeric(10,2) default 0")
    @Builder.Default
    private BigDecimal minQuantity = BigDecimal.ZERO;

    @Column(name = "whites_applicable", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean whitesApplicable = false;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServiceWashModePrice> prices = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    public void validateBusinessRules() {
        if (this.minQuantity == null || this.minQuantity.signum() < 0) {
            throw new BusinessException("Tối thiểu không được âm", "minQuantity");
        }
        int maxScale = this.unit == ServiceUnit.KG ? 2 : 0;
        if (this.minQuantity.stripTrailingZeros().scale() > maxScale) {
            throw new BusinessException(maxScale == 0
                    ? "Tối thiểu phải là số nguyên với đơn vị này"
                    : "Tối thiểu tối đa 2 chữ số thập phân", "minQuantity");
        }
    }

    public void deactivate() {
        if (this.status == ServiceStatus.INACTIVE) {
            throw new BusinessException("Dịch vụ đã ở trạng thái Ngừng hoạt động");
        }
        this.status = ServiceStatus.INACTIVE;
    }

    public void activate() {
        if (this.status == ServiceStatus.ACTIVE) {
            throw new BusinessException("Dịch vụ đã ở trạng thái Hoạt động");
        }
        this.status = ServiceStatus.ACTIVE;
    }
}
