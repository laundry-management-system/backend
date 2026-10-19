package com.laundry.modules.customers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customers_customer_code", columnNames = "customer_code"),
        @UniqueConstraint(name = "uk_customers_phone", columnNames = "phone")
})
@SequenceGenerator(name = "customer_id_generator", sequenceName = "customers_id_seq", allocationSize = 1)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_id_generator")
    private Long id;

    @Column(name = "customer_code", nullable = false, length = 12, unique = true)
    private String customerCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 10, unique = true)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String note;

    @Column(name = "is_vip", nullable = false)
    private boolean vip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Customer create(String fullName, String phone, String address, String note, Long createdBy) {
        Customer customer = new Customer();
        customer.fullName = fullName.trim();
        customer.phone = phone;
        customer.address = address;
        customer.note = note;
        customer.createdBy = createdBy;
        return customer;
    }

    public void updateProfile(String fullName, String address, String note) {
        this.fullName = fullName.trim();
        this.address = address;
        this.note = note;
    }

    public void changeStatus(CustomerStatus status) {
        this.status = status;
    }

    @PrePersist
    private void assignCustomerCode() {
        if (customerCode == null && id != null) {
            customerCode = "KH%06d".formatted(id);
        }
    }
}
