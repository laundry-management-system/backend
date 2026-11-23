package com.laundry.modules.customer.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.ResourceConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_registrations")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CustomerRegistration extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 10)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public void activate(Long customerId, String admin) {
        requirePending();
        this.status = RegistrationStatus.ACTIVATED;
        this.customerId = customerId;
        markReviewed(admin);
    }

    public void reject(String admin) {
        requirePending();
        this.status = RegistrationStatus.REJECTED;
        markReviewed(admin);
    }

    public void unlinkCustomer() {
        this.customerId = null;
    }

    public void requirePending() {
        if (status != RegistrationStatus.PENDING) {
            throw new ResourceConflictException("Yêu cầu đã được xử lý");
        }
    }

    private void markReviewed(String admin) {
        this.reviewedBy = admin;
        this.reviewedAt = Instant.now();
    }
}
