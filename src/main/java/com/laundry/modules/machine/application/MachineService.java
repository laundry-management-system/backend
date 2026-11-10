package com.laundry.modules.machine.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.machine.application.dto.ChangeMachineStatusRequest;
import com.laundry.modules.machine.application.dto.CreateMachineRequest;
import com.laundry.modules.machine.application.dto.MachineResponse;
import com.laundry.modules.machine.application.dto.UpdateMachineRequest;
import com.laundry.modules.machine.domain.Machine;
import com.laundry.modules.machine.domain.MachineRepository;
import com.laundry.modules.machine.domain.MachineStatus;
import com.laundry.modules.machine.domain.MachineType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public MachineResponse createMachine(CreateMachineRequest request) {
        String code = request.machineCode() != null && !request.machineCode().isBlank()
                ? request.machineCode().trim().toUpperCase()
                : generateMachineCode(request.type());

        if (machineRepository.existsByMachineCodeIgnoreCase(code)) {
            throw new BusinessException("Machine code '" + code + "' already exists");
        }

        Machine machine = Machine.builder()
                .machineCode(code)
                .name(request.name().trim())
                .type(request.type())
                .capacityKg(request.capacityKg())
                .status(MachineStatus.READY)
                .note(request.note() != null ? request.note().trim() : null)
                .build();

        machine.validateBusinessRules();

        Machine saved = machineRepository.save(machine);
        audit(AuditAction.MACHINE_MANAGE, saved, null, snapshot(saved), null);
        return MachineResponse.from(saved);
    }

    @Transactional
    public MachineResponse updateMachine(Long id, UpdateMachineRequest request) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Machine not found with ID: " + id));
        Map<String, Object> before = snapshot(machine);

        machine.updateDetails(
                request.name(),
                request.type(),
                request.capacityKg(),
                request.note()
        );

        Machine saved = machineRepository.save(machine);
        audit(AuditAction.MACHINE_MANAGE, saved, before, snapshot(saved), null);
        return MachineResponse.from(saved);
    }

    @Transactional
    public MachineResponse changeStatus(Long id, ChangeMachineStatusRequest request, Authentication authentication) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Machine not found with ID: " + id));

        MachineStatus targetStatus = request.status();
        boolean isAdmin = isAdmin(authentication);

        // UC18 vs UC14: Only Admin can retire a machine or reactivate from retired
        if ((targetStatus == MachineStatus.RETIRED || machine.getStatus() == MachineStatus.RETIRED) && !isAdmin) {
            throw new AccessDeniedException("Only administrators can retire or restore machines (UC18)");
        }

        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        if (targetStatus == MachineStatus.MAINTENANCE && reason == null) {
            throw new BusinessException("Bắt buộc nhập lý do khi chuyển máy sang Bảo trì", "reason");
        }

        MachineStatus oldStatus = machine.getStatus();
        machine.changeStatus(targetStatus);

        Machine saved = machineRepository.save(machine);

        // Log audit event per QĐ10 and BM8
        audit(AuditAction.MACHINE_STATUS, saved, Map.of("status", oldStatus.name()),
                Map.of("status", targetStatus.name()), reason);

        return MachineResponse.from(saved);
    }

    private void audit(AuditAction action, Machine m, Map<String, Object> before, Map<String, Object> after,
                       String reason) {
        auditLogService.log(new AuditEntry(action, EntityType.MACHINE, m.getId(), before, after, reason,
                m.getMachineCode()));
    }

    private static Map<String, Object> snapshot(Machine m) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("machineCode", m.getMachineCode());
        data.put("name", m.getName());
        data.put("type", m.getType().name());
        data.put("capacityKg", m.getCapacityKg());
        data.put("status", m.getStatus().name());
        data.put("note", m.getNote());
        return data;
    }

    @Transactional(readOnly = true)
    public MachineResponse getMachineById(Long id) {
        return machineRepository.findById(id)
                .map(MachineResponse::from)
                .orElseThrow(() -> new NotFoundException("Machine not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public MachineResponse getMachineByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return machineRepository.findByMachineCodeIgnoreCase(cleanCode)
                .map(MachineResponse::from)
                .orElseThrow(() -> new NotFoundException("Machine not found with code: " + cleanCode));
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> listMachines(MachineType type, MachineStatus status) {
        return machineRepository.findMachines(type, status).stream()
                .map(MachineResponse::from)
                .toList();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String generateMachineCode(MachineType type) {
        String prefix = type == MachineType.WASHER ? "W" : "D";
        long count = machineRepository.countByType(type) + 1;
        String candidate = String.format("%s%02d", prefix, count);

        long counter = count;
        while (machineRepository.existsByMachineCodeIgnoreCase(candidate)) {
            counter++;
            candidate = String.format("%s%02d", prefix, counter);
        }
        return candidate;
    }
}
