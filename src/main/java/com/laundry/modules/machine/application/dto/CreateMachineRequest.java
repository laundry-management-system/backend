package com.laundry.modules.machine.application.dto;

import com.laundry.modules.machine.domain.MachineType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMachineRequest(
        @NotBlank(message = "Machine name must not be blank")
        @Size(max = 50, message = "Machine name must not exceed 50 characters")
        String name,

        @NotNull(message = "Machine type must not be null")
        MachineType type,

        @NotNull(message = "Machine capacity must not be null")
        @DecimalMin(value = "0.1", message = "Machine capacity must be greater than 0 kg")
        BigDecimal capacityKg,

        @Size(max = 255, message = "Note must not exceed 255 characters")
        String note,
        @NotBlank(message = "Vui lòng nhập hãng máy")
        @Size(max = 50, message = "Hãng máy tối đa 50 ký tự")
        String brand,
        @Size(max = 50, message = "Model tối đa 50 ký tự")
        String model,
        @Size(max = 50, message = "Số serial tối đa 50 ký tự")
        String serialNumber,
        @Size(max = 100, message = "Vị trí tối đa 100 ký tự")
        String location,
        @PastOrPresent(message = "Ngày mua không được ở tương lai")
        LocalDate purchaseDate,
        LocalDate warrantyUntil,
        Long imageId
) {}
