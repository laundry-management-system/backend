package com.laundry.modules.machine.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.machine.application.dto.ChangeMachineStatusRequest;
import com.laundry.modules.machine.application.dto.MachineRequest;
import com.laundry.modules.machine.application.dto.MachineResponse;
import com.laundry.modules.machine.application.dto.RetireMachineRequest;
import com.laundry.modules.machine.domain.Machine;
import com.laundry.modules.machine.domain.MachineRepository;
import com.laundry.modules.machine.domain.MachineStatus;
import com.laundry.modules.machine.domain.MachineType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<MachineResponse> list(MachineType type, MachineStatus status) {
        return machineRepository.search(type, status).stream().map(MachineResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MachineResponse get(Long id) {
        return MachineResponse.from(findById(id));
    }

    @Transactional
    public MachineResponse create(MachineRequest request) {
        int next = machineRepository.maxCodeNumber(request.type()) + 1;
        Machine machine = Machine.builder()
                .machineCode(String.format("%s%02d", request.type().getCodePrefix(), next))
                .name(request.name().trim())
                .type(request.type())
                .capacityKg(request.capacityKg())
                .note(trimToNull(request.note()))
                .status(MachineStatus.READY)
                .build();
        try {
            machine = machineRepository.saveAndFlush(machine);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Mã máy vừa được tạo bởi thao tác khác, vui lòng thử lại");
        }
        audit(AuditAction.MACHINE_MANAGE, machine, null, snapshot(machine), null);
        return MachineResponse.from(machine);
    }

    @Transactional
    public MachineResponse update(Long id, MachineRequest request) {
        Machine machine = findById(id);
        if (request.type() != machine.getType()) {
            throw new BusinessException("Không đổi được loại máy; hãy thêm máy mới", "type");
        }
        if (machine.getStatus() == MachineStatus.RETIRED) {
            throw new BusinessException("Máy đã ngừng sử dụng, không sửa được");
        }
        Map<String, Object> before = snapshot(machine);
        machine.setName(request.name().trim());
        machine.setCapacityKg(request.capacityKg());
        machine.setNote(trimToNull(request.note()));
        machine = machineRepository.saveAndFlush(machine);
        audit(AuditAction.MACHINE_MANAGE, machine, before, snapshot(machine), null);
        return MachineResponse.from(machine);
    }

    @Transactional
    public MachineResponse changeStatus(Long id, ChangeMachineStatusRequest request) {
        Machine machine = findById(id);
        Map<String, Object> before = snapshot(machine);
        String reason = trimToNull(request.reason());
        machine.changeOperationalStatus(request.status(), reason);
        machine = machineRepository.saveAndFlush(machine);
        audit(AuditAction.MACHINE_STATUS, machine, before, snapshot(machine), reason);
        return MachineResponse.from(machine);
    }

    @Transactional
    public MachineResponse retire(Long id, RetireMachineRequest request) {
        Machine machine = findById(id);
        Map<String, Object> before = snapshot(machine);
        machine.retire();
        machine = machineRepository.saveAndFlush(machine);
        audit(AuditAction.MACHINE_MANAGE, machine, before, snapshot(machine),
                request == null ? null : trimToNull(request.reason()));
        return MachineResponse.from(machine);
    }

    private Machine findById(Long id) {
        return machineRepository.findById(id).orElseThrow(() -> NotFoundException.of("Máy", id));
    }

    private void audit(AuditAction action, Machine m, Map<String, Object> before, Map<String, Object> after,
                       String reason) {
        auditLogService.log(new AuditEntry(action, EntityType.MACHINE, m.getId(), before, after, reason,
                m.getMachineCode()));
    }

    private static Map<String, Object> snapshot(Machine m) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("machineCode", m.getMachineCode());
        data.put("name", m.getName());
        data.put("type", m.getType().name());
        data.put("capacityKg", m.getCapacityKg());
        data.put("status", m.getStatus().name());
        data.put("note", m.getNote());
        return data;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
