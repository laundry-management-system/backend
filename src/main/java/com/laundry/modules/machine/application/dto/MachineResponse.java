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
        String typeDescription,
        BigDecimal capacityKg,
        MachineStatus status,
        String statusDescription,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
    public static MachineResponse from(Machine m) {
        return new MachineResponse(m.getId(), m.getMachineCode(), m.getName(), m.getType(),
                m.getType().getDescription(), m.getCapacityKg(), m.getStatus(), m.getStatus().getDescription(),
                m.getNote(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
