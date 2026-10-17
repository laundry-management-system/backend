package com.laundry.modules.customer.domain;

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

@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_phone", columnList = "phone"),
                @Index(name = "idx_customers_code", columnList = "customer_code")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Customer extends BaseEntity {

    @Column(name = "customer_code", nullable = false, unique = true, length = 12)
    private String customerCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", nullable = false, unique = true, length = 10)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "is_vip", nullable = false)
    @Builder.Default
    private Boolean isVip = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    public void updateInfo(String fullName, String address, String note) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("Customer full name must not be blank (BR-B02)");
        }
        this.fullName = fullName.trim();
        this.address = address != null ? address.trim() : null;
        this.note = note != null ? note.trim() : null;
    }

    public void updatePhone(String newPhone) {
        if (newPhone == null || !newPhone.matches("^0\\d{9}$")) {
            throw new BusinessException("Phone number must contain 10 digits starting with 0 (BR-B01)");
        }
        this.phone = newPhone.trim();
    }

    public void deactivate() {
        if (this.status == CustomerStatus.INACTIVE) {
            throw new BusinessException("Customer is already inactive");
        }
        this.status = CustomerStatus.INACTIVE;
    }

    public void activate() {
        if (this.status == CustomerStatus.ACTIVE) {
            throw new BusinessException("Customer is already active");
        }
        this.status = CustomerStatus.ACTIVE;
    }

    public void setVipStatus(boolean vip) {
        this.isVip = vip;
    }
}
