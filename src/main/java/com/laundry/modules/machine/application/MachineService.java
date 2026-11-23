package com.laundry.modules.machine.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.document.application.DocumentService;
import com.laundry.modules.document.domain.Document;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.machine.application.dto.ChangeMachineStatusRequest;
import com.laundry.modules.machine.application.dto.CreateMachineBatchRequest;
import com.laundry.modules.machine.application.dto.CreateMachineRequest;
import com.laundry.modules.machine.application.dto.MachineResponse;
import com.laundry.modules.machine.application.dto.UpdateMachineRequest;
import com.laundry.modules.machine.domain.Machine;
import com.laundry.modules.machine.domain.MachineCode;
import com.laundry.modules.machine.domain.MachineRepository;
import com.laundry.modules.machine.domain.MachineStatus;
import com.laundry.modules.machine.domain.MachineType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;

    @Transactional
    public MachineResponse createMachine(CreateMachineRequest request) {
        return createMachines(new CreateMachineBatchRequest(request, 1, null)).get(0);
    }

    /** Tạo n máy cùng thông tin; mã W-<hãng>-00X tự cấp, tên thêm số thứ tự khi n > 1. Lỗi một máy thì không tạo máy nào. */
    @Transactional
    public List<MachineResponse> createMachines(CreateMachineBatchRequest batch) {
        CreateMachineRequest request = batch.machine();
        int quantity = batch.quantity();
        List<String> serials = serialsFor(batch);
        String prefix = MachineCode.prefix(request.type(), request.brand());
        int next = machineRepository.findCodesStartingWith(prefix).stream()
                .mapToInt(code -> MachineCode.sequenceOf(code, prefix))
                .max().orElse(0) + 1;
        String baseName = request.name().trim();

        List<MachineResponse> created = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            int sequence = next + i;
            String name = quantity == 1 ? baseName : baseName + " " + sequence;
            if (name.length() > 50) {
                throw new BusinessException("Tên máy quá dài để thêm số thứ tự (tối đa 50 ký tự)", "name");
            }
            Machine saved = saveNew(request, MachineCode.format(prefix, sequence), name, serials.get(i));
            created.add(MachineResponse.from(saved));
        }
        return created;
    }

    /** Serial cho từng máy: 1 máy dùng ô serial của form, nhiều máy dùng danh sách riêng. */
    private List<String> serialsFor(CreateMachineBatchRequest batch) {
        if (batch.quantity() == 1) {
            return Collections.singletonList(batch.machine().serialNumber());
        }
        if (batch.machine().serialNumber() != null && !batch.machine().serialNumber().isBlank()) {
            throw new BusinessException("Nhập serial riêng cho từng máy", "serialNumber");
        }
        List<String> given = batch.serialNumbers() == null ? List.of() : batch.serialNumbers();
        if (given.size() > batch.quantity()) {
            throw new BusinessException("Số ô serial nhiều hơn số lượng máy", "serialNumbers");
        }
        List<String> serials = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < batch.quantity(); i++) {
            String serial = Machine.normalizeSerial(i < given.size() ? given.get(i) : null);
            String field = "serialNumbers." + i;
            if (serial != null && !seen.add(serial)) {
                throw new BusinessException("Số serial bị trùng trong danh sách", field);
            }
            if (serial != null && machineRepository.existsBySerialNumber(serial)) {
                throw new BusinessException("Số serial đã được dùng cho máy khác", field);
            }
            serials.add(serial);
        }
        return serials;
    }

    private Machine saveNew(CreateMachineRequest request, String code, String name, String serialNumber) {
        Machine machine = Machine.builder()
                .machineCode(code)
                .name(name)
                .type(request.type())
                .capacityKg(request.capacityKg())
                .status(MachineStatus.READY)
                .note(request.note() != null ? request.note().trim() : null)
                .build();

        machine.validateBusinessRules();
        applyAssetInfo(machine, request.brand(), request.model(), serialNumber, request.location(),
                request.purchaseDate(), request.warrantyUntil(), request.imageId());

        // saveAndFlush để lần cấp mã kế tiếp thấy máy vừa tạo
        return machineRepository.saveAndFlush(machine);
    }

    @Transactional
    public MachineResponse updateMachine(Long id, UpdateMachineRequest request) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Machine not found with ID: " + id));

        machine.updateDetails(
                request.name(),
                request.type(),
                request.capacityKg(),
                request.note()
        );
        applyAssetInfo(machine, request.brand(), request.model(), request.serialNumber(), request.location(),
                request.purchaseDate(), request.warrantyUntil(), request.imageId());

        Machine saved = machineRepository.save(machine);
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

        MachineStatus oldStatus = machine.getStatus();
        machine.changeStatus(targetStatus);

        Machine saved = machineRepository.save(machine);

        // Log audit event per QĐ10 and BM8
        auditLogService.log(new AuditEntry(
                AuditAction.MACHINE_STATUS,
                EntityType.MACHINE,
                saved.getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", targetStatus.name()),
                request.reason()
        ));

        return MachineResponse.from(saved);
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

    private void applyAssetInfo(Machine machine, String brand, String model, String serialNumber, String location,
                                LocalDate purchaseDate, LocalDate warrantyUntil, Long imageId) {
        String serial = Machine.normalizeSerial(serialNumber);
        boolean taken = serial != null && (machine.getId() == null
                ? machineRepository.existsBySerialNumber(serial)
                : machineRepository.existsBySerialNumberAndIdNot(serial, machine.getId()));
        if (taken) {
            throw new BusinessException("Số serial đã được dùng cho máy khác", "serialNumber");
        }
        Document image = imageId != null ? documentService.requireImage(imageId) : null;
        machine.updateAssetInfo(brand, model, serial, location, purchaseDate, warrantyUntil, image);
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
