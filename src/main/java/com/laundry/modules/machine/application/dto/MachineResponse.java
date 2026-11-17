package com.laundry.modules.machine.application.dto;

import com.laundry.modules.document.application.dto.DocumentResponse;
import com.laundry.modules.machine.domain.Machine;
import com.laundry.modules.machine.domain.MachineStatus;
import com.laundry.modules.machine.domain.MachineType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MachineResponse(
        Long id,
        String machineCode,
        String name,
        MachineType type,
        BigDecimal capacityKg,
        MachineStatus status,
        String note,
        String brand,
        String model,
        String serialNumber,
        String location,
        LocalDate purchaseDate,
        LocalDate warrantyUntil,
        Long imageId,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt
) {
    public static MachineResponse from(Machine machine) {
        return new MachineResponse(
                machine.getId(),
                machine.getMachineCode(),
                machine.getName(),
                machine.getType(),
                machine.getCapacityKg(),
                machine.getStatus(),
                machine.getNote(),
                machine.getBrand(),
                machine.getModel(),
                machine.getSerialNumber(),
                machine.getLocation(),
                machine.getPurchaseDate(),
                machine.getWarrantyUntil(),
                machine.getImage() != null ? machine.getImage().getId() : null,
                machine.getImage() != null ? DocumentResponse.urlOf(machine.getImage()) : null,
                machine.getCreatedAt(),
                machine.getUpdatedAt()
        );
    }
}
