package com.laundry.modules.service.application;

import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.service.application.dto.CreateServiceRequest;
import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.application.dto.UpdateServiceRequest;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.ServiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.application.dto.AuditLogDetailResponse;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;

import java.util.LinkedHashMap;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LaundryServiceService {

    private final LaundryServiceRepository serviceRepository;
    private final AuditLogService auditLogService;

    public Page<ServiceResponse> listServices(String search, ServiceStatus status, Pageable pageable) {
        String term = search != null ? search.trim() : "";
        return serviceRepository.searchServices(term, status, pageable).map(ServiceResponse::from);
    }

    /** Toàn bộ dịch vụ đang hoạt động, cho ô chọn dịch vụ khi phân loại đơn. */
    public List<ServiceResponse> listActiveServices() {
        return serviceRepository.findByStatusOrderByServiceCodeAsc(ServiceStatus.ACTIVE).stream()
                .map(ServiceResponse::from)
                .toList();
    }

    public ServiceResponse getServiceById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceResponse::from)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));
    }

    public ServiceResponse getServiceByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return serviceRepository.findByServiceCode(cleanCode)
                .map(ServiceResponse::from)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ mã", cleanCode));
    }

    @Transactional
    public ServiceResponse createService(CreateServiceRequest request) {
        LaundryService service = LaundryService.builder()
                .serviceCode("TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 6))
                .name(request.name().trim())
                .unit(request.unit())
                .currentPrice(request.currentPrice())
                .standardHours(request.standardHours())
                .expressSupported(request.expressSupported() != null ? request.expressSupported() : false)
                .expressSurcharge(request.expressSurcharge())
                .expressHours(request.expressHours())
                .minQuantity(request.minQuantity() != null ? request.minQuantity() : BigDecimal.ZERO)
                .whitesApplicable(Boolean.TRUE.equals(request.whitesApplicable()))
                .status(ServiceStatus.ACTIVE)
                .build();

        service.validateBusinessRules();

        LaundryService saved = serviceRepository.saveAndFlush(service);
        saved.setServiceCode(String.format("DV%02d",saved.getId()));

        // Giá khởi tạo là dòng đầu tiên của lịch sử giá
        audit(AuditAction.PRICE_CHANGE, saved, null, priceSnapshot(saved), "Tạo dịch vụ");
        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse updateService(Long id, UpdateServiceRequest request) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));
        Map<String, Object> priceBefore = priceSnapshot(service);
        ServiceStatus statusBefore = service.getStatus();

        if (request.name() != null && !request.name().trim().isEmpty()) {
            service.setName(request.name().trim());
        }
        if (request.unit() != null) {
            service.setUnit(request.unit());
        }
        if (request.currentPrice() != null) {
            service.setCurrentPrice(request.currentPrice());
        }
        if (request.standardHours() != null) {
            service.setStandardHours(request.standardHours());
        }
        if (request.expressSupported() != null) {
            service.setExpressSupported(request.expressSupported());
        }

        if (request.expressSurcharge() != null || (request.expressSupported() != null && !request.expressSupported())) {
            service.setExpressSurcharge(request.expressSurcharge());
        }
        if (request.expressHours() != null || (request.expressSupported() != null && !request.expressSupported())) {
            service.setExpressHours(request.expressHours());
        }

        if (request.minQuantity() != null) {
            service.setMinQuantity(request.minQuantity());
        }
        if (request.whitesApplicable() != null) {
            service.setWhitesApplicable(request.whitesApplicable());
        }

        service.validateBusinessRules();

        if (request.status() != null && request.status() != service.getStatus()) {
            applyStatus(service, request.status());
        }

        this.serviceRepository.save(service);

        // BR-C02: đổi đơn giá / phụ phí / tối thiểu thì ghi lịch sử giá kèm lý do
        Map<String, Object> priceAfter = priceSnapshot(service);
        String reason = request.priceChangeReason() == null || request.priceChangeReason().isBlank()
                ? null : request.priceChangeReason().trim();
        if (!priceAfter.equals(priceBefore)) {
            audit(AuditAction.PRICE_CHANGE, service, priceBefore, priceAfter, reason);
        }
        if (service.getStatus() != statusBefore) {
            audit(AuditAction.SERVICE_STATUS, service, Map.of("status", statusBefore.name()),
                    Map.of("status", service.getStatus().name()), reason);
        }
        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse changeStatus(Long id, ServiceStatus status) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));
        ServiceStatus before = service.getStatus();

        applyStatus(service, status);

        LaundryService saved = serviceRepository.save(service);
        audit(AuditAction.SERVICE_STATUS, saved, Map.of("status", before.name()),
                Map.of("status", saved.getStatus().name()), null);
        return ServiceResponse.from(saved);
    }

    /** Lịch sử giá của dịch vụ (WF-H03), mới nhất trước. */
    @Transactional(readOnly = true)
    public List<AuditLogDetailResponse> priceHistory(Long id) {
        if (!serviceRepository.existsById(id)) throw NotFoundException.of("Dịch vụ", id);
        return auditLogService.history(EntityType.SERVICE, id, Set.of(AuditAction.PRICE_CHANGE));
    }

    private void audit(AuditAction action, LaundryService service, Map<String, Object> before,
                       Map<String, Object> after, String reason) {
        auditLogService.log(new AuditEntry(action, EntityType.SERVICE, service.getId(), before, after, reason,
                service.getServiceCode()));
    }

    /** Các trường ảnh hưởng tới giá; so sánh BigDecimal theo chuỗi rút gọn để 30000 = 30000.00. */
    private static Map<String, Object> priceSnapshot(LaundryService s) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentPrice", plain(s.getCurrentPrice()));
        data.put("expressSupported", s.getExpressSupported());
        data.put("expressSurcharge", plain(s.getExpressSurcharge()));
        data.put("minQuantity", plain(s.getMinQuantity()));
        data.put("unit", s.getUnit() == null ? null : s.getUnit().name());
        return data;
    }

    private static String plain(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private void applyStatus(LaundryService service, ServiceStatus status) {
        if (status == ServiceStatus.INACTIVE) {
            service.deactivate();
        } else if (status == ServiceStatus.ACTIVE) {
            service.activate();
        }
    }
}
