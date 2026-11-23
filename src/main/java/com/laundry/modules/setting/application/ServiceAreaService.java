package com.laundry.modules.setting.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.setting.application.dto.ServiceAreaRequest;
import com.laundry.modules.setting.application.dto.ServiceAreaResponse;
import com.laundry.modules.setting.domain.ServiceArea;
import com.laundry.modules.setting.domain.ServiceAreaRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý khu vực giao nhận (FR-029a → FR-029f). Không có xoá. */
@Service
@RequiredArgsConstructor
public class ServiceAreaService {

    private static final String DUPLICATE_NAME = "Tên khu vực đã tồn tại";

    private final ServiceAreaRepository serviceAreaRepository;
    private final AuditLogService auditLogService;

    public List<ServiceAreaResponse> list() {
        return serviceAreaRepository.findAllByOrderByIdAsc().stream().map(ServiceAreaResponse::from).toList();
    }

    @Transactional
    public ServiceAreaResponse create(ServiceAreaRequest request) {
        String name = request.name().trim();
        if (serviceAreaRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(DUPLICATE_NAME, "name");
        }
        ServiceArea saved = saveUnique(new ServiceArea(name, request.pickupFee(), request.deliveryFee()));
        audit(saved.getId(), null, snapshot(saved));
        return ServiceAreaResponse.from(saved);
    }

    @Transactional
    public ServiceAreaResponse update(Long id, ServiceAreaRequest request) {
        ServiceArea area = find(id);
        String name = request.name().trim();
        if (serviceAreaRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(DUPLICATE_NAME, "name");
        }
        // So phí bằng compareTo để 15000 và 15000.0 không bị coi là đổi
        boolean changed = !area.getName().equals(name)
                || area.getPickupFee().compareTo(request.pickupFee()) != 0
                || area.getDeliveryFee().compareTo(request.deliveryFee()) != 0;
        if (changed) {
            Map<String, Object> before = snapshot(area);
            area.update(name, request.pickupFee(), request.deliveryFee());
            saveUnique(area);
            audit(id, before, snapshot(area));
        }
        return ServiceAreaResponse.from(area);
    }

    @Transactional
    public ServiceAreaResponse changeActive(Long id, boolean active) {
        ServiceArea area = find(id);
        if (area.isActive() != active) {
            Map<String, Object> before = snapshot(area);
            area.changeActive(active);
            audit(id, before, snapshot(area));
        }
        return ServiceAreaResponse.from(area);
    }

    private ServiceArea find(Long id) {
        return serviceAreaRepository.findById(id).orElseThrow(() -> NotFoundException.of("Khu vực", id));
    }

    // Hai Admin lưu cùng tên cùng lúc: unique index lower(name) chặn, trả cùng lỗi như khi kiểm trước (mẫu UserService)
    private ServiceArea saveUnique(ServiceArea area) {
        try {
            return serviceAreaRepository.saveAndFlush(area);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(DUPLICATE_NAME, "name");
        }
    }

    private Map<String, Object> snapshot(ServiceArea area) {
        return Map.of(
                "name", area.getName(),
                "pickupFee", area.getPickupFee(),
                "deliveryFee", area.getDeliveryFee(),
                "active", area.isActive());
    }

    private void audit(Long id, Map<String, Object> oldData, Map<String, Object> newData) {
        auditLogService.log(new AuditEntry(AuditAction.SERVICE_AREA_MANAGE, EntityType.SERVICE_AREA, id, oldData, newData, null));
    }
}
