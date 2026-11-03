package com.laundry.modules.machine.presentation;

import com.laundry.modules.machine.application.MachineService;
import com.laundry.modules.machine.application.dto.ChangeMachineStatusRequest;
import com.laundry.modules.machine.application.dto.CreateMachineRequest;
import com.laundry.modules.machine.application.dto.MachineResponse;
import com.laundry.modules.machine.application.dto.UpdateMachineRequest;
import com.laundry.modules.machine.domain.MachineStatus;
import com.laundry.modules.machine.domain.MachineType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Machine Management", description = "Washing and drying machine management (M3 - Member 2: UC14, UC18, BM8, QĐ10, BR-E)")
@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @Operation(summary = "View machine list (UC14 / BM8 - Staff & Admin)")
    @GetMapping
    public List<MachineResponse> listMachines(
            @RequestParam(required = false) MachineType type,
            @RequestParam(required = false) MachineStatus status
    ) {
        return machineService.listMachines(type, status);
    }

    @Operation(summary = "Get machine details by ID (BM8)")
    @GetMapping("/{id}")
    public MachineResponse getMachineById(@PathVariable Long id) {
        return machineService.getMachineById(id);
    }

    @Operation(summary = "Get machine details by code (Wxx / Dxx)")
    @GetMapping("/by-code/{code}")
    public MachineResponse getMachineByCode(@PathVariable String code) {
        return machineService.getMachineByCode(code);
    }

    @Operation(summary = "Add new machine (UC18 - Admin only, BR-E)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MachineResponse createMachine(@Valid @RequestBody CreateMachineRequest request) {
        return machineService.createMachine(request);
    }

    @Operation(summary = "Update machine details (UC18 - Admin only, BR-E)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MachineResponse updateMachine(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMachineRequest request
    ) {
        return machineService.updateMachine(id, request);
    }

    @Operation(summary = "Change machine status (UC14: READY ⇄ MAINTENANCE by Staff/Admin; UC18: RETIRED by Admin only)")
    @PatchMapping("/{id}/status")
    public MachineResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeMachineStatusRequest request,
            Authentication authentication
    ) {
        return machineService.changeStatus(id, request, authentication);
    }
}
