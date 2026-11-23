package com.laundry.modules.machine.domain;

import com.laundry.common.entity.BaseEntity;
import com.laundry.common.exception.BusinessException;
import com.laundry.modules.document.domain.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "machines",
        indexes = {
                @Index(name = "idx_machines_code", columnList = "machine_code"),
                @Index(name = "idx_machines_status", columnList = "status"),
                @Index(name = "idx_machines_type", columnList = "type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Machine extends BaseEntity {

    @Column(name = "machine_code", nullable = false, unique = true, length = 10)
    private String machineCode;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MachineType type;

    @Column(name = "capacity_kg", nullable = false, precision = 5, scale = 1)
    private BigDecimal capacityKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MachineStatus status = MachineStatus.READY;

    @Column(name = "note")
    private String note;

    @Column(name = "brand", length = 50)
    private String brand;

    @Column(name = "model", length = 50)
    private String model;

    // Lưu chữ hoa để unique không phân biệt hoa thường
    @Column(name = "serial_number", length = 50, unique = true)
    private String serialNumber;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_until")
    private LocalDate warrantyUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_document_id")
    private Document image;

    public void validateBusinessRules() {
        if (capacityKg == null || capacityKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Machine capacity must be greater than 0 kg");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("Machine name must not be blank");
        }
        if (type == null) {
            throw new BusinessException("Machine type must not be null");
        }
        if (purchaseDate != null && warrantyUntil != null && warrantyUntil.isBefore(purchaseDate)) {
            throw new BusinessException("Ngày hết bảo hành phải sau ngày mua", "warrantyUntil");
        }
    }

    public void updateDetails(String name, MachineType type, BigDecimal capacityKg, String note) {
        if (this.status == MachineStatus.RETIRED) {
            throw new BusinessException("Cannot update details of a retired machine");
        }
        this.name = name.trim();
        this.type = type;
        this.capacityKg = capacityKg;
        this.note = note != null ? note.trim() : null;

        validateBusinessRules();
    }

    public void updateAssetInfo(String brand, String model, String serialNumber, String location,
                                LocalDate purchaseDate, LocalDate warrantyUntil, Document image) {
        this.brand = blankToNull(brand);
        this.model = blankToNull(model);
        this.serialNumber = normalizeSerial(serialNumber);
        this.location = blankToNull(location);
        this.purchaseDate = purchaseDate;
        this.warrantyUntil = warrantyUntil;
        this.image = image;
        validateBusinessRules();
    }

    public static String normalizeSerial(String serialNumber) {
        String s = blankToNull(serialNumber);
        return s == null ? null : s.toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public void changeStatus(MachineStatus newStatus) {
        if (this.status == newStatus) {
            throw new BusinessException("Machine is already in status " + newStatus);
        }
        if (this.status == MachineStatus.RETIRED && newStatus != MachineStatus.READY) {
            throw new BusinessException("Retired machine can only be reactivated to READY status");
        }
        this.status = newStatus;
    }
}
