package com.laundry.modules.machine.application.dto;

import com.laundry.modules.machine.domain.Machine;
import com.laundry.modules.machine.domain.MachineStatus;
import com.laundry.modules.machine.domain.MachineType;

import java.math.BigDecimal;
import java.time.Instant;

public record MachineResponse(
        Long id,
        String machineCode,
        String name,
        MachineType type,
        BigDecimal capacityKg,
        MachineStatus status,
        String note,
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
                machine.getCreatedAt(),
                machine.getUpdatedAt()
        );
    }
}
