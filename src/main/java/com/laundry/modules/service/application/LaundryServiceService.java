package com.laundry.modules.service.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.service.application.dto.CreateServiceRequest;
import com.laundry.modules.service.application.dto.CreateServiceRequest.PriceLine;
import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.application.dto.UpdateServiceRequest;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.ServicePriceSnapshot;
import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.service.domain.ServiceWashModePrice;
import com.laundry.modules.service.domain.WashMode;
import com.laundry.modules.service.domain.WashModeRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LaundryServiceService {

    private final LaundryServiceRepository serviceRepository;
    private final AuditLogService auditLogService;
    private final WashModeRepository washModeRepository;

    @Transactional(readOnly = true)
    public Page<ServiceResponse> listServices(String search, ServiceStatus status, Pageable pageable) {
        String term = search != null ? search.trim() : "";
        return serviceRepository.searchServices(term, status, pageable).map(ServiceResponse::from);
    }

    /** Toàn bộ dịch vụ đang hoạt động, cho ô chọn dịch vụ khi phân loại đơn. */
    @Transactional(readOnly = true)
    public List<ServiceResponse> listActiveServices() {
        return serviceRepository.findByStatusOrderByServiceCodeAsc(ServiceStatus.ACTIVE).stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceResponse::from)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));
    }

    @Transactional(readOnly = true)
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
                .minQuantity(request.minQuantity() != null ? request.minQuantity() : BigDecimal.ZERO)
                .whitesApplicable(Boolean.TRUE.equals(request.whitesApplicable()))
                .status(ServiceStatus.ACTIVE)
                .build();

        service.validateBusinessRules();
        applyPrices(service, request.prices());

        LaundryService saved = serviceRepository.saveAndFlush(service);
        saved.setServiceCode(String.format("DV%02d",saved.getId()));

        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse updateService(Long id, UpdateServiceRequest request) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));
        ServicePriceSnapshot before = ServicePriceSnapshot.of(service);

        if (request.name() != null && !request.name().trim().isEmpty()) {
            service.setName(request.name().trim());
        }
        if (request.unit() != null) {
            service.setUnit(request.unit());
        }
        if (request.minQuantity() != null) {
            service.setMinQuantity(request.minQuantity());
        }
        if (request.whitesApplicable() != null) {
            service.setWhitesApplicable(request.whitesApplicable());
        }

        service.validateBusinessRules();
        if (request.prices() != null) {
            applyPrices(service, request.prices());
        }

        if (request.status() != null && request.status() != service.getStatus()) {
            applyStatus(service, request.status());
        }

        logPriceChange(service, before, request.priceChangeReason());
        this.serviceRepository.save(service);

        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse changeStatus(Long id, ServiceStatus status, String reason) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));
        ServicePriceSnapshot before = ServicePriceSnapshot.of(service);

        applyStatus(service, status);
        logPriceChange(service, before, reason);

        LaundryService saved = serviceRepository.save(service);
        return ServiceResponse.from(saved);
    }

    /**
     * Thay liên kết tới các hình thức đang hoạt động bằng {@code lines}; liên kết tới hình thức đã ngừng giữ nguyên
     * (FR-001b, FR-037). Sửa giá tại chỗ để không xoá rồi chèn lại cùng cặp (service, mode).
     */
    private void applyPrices(LaundryService service, List<PriceLine> lines) {
        Map<Long, WashMode> modes = washModeRepository
                .findAllById(lines.stream().map(PriceLine::washModeId).toList()).stream()
                .collect(Collectors.toMap(WashMode::getId, Function.identity()));
        Map<Long, BigDecimal> wanted = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            PriceLine line = lines.get(i);
            WashMode mode = modes.get(line.washModeId());
            if (mode == null || !Boolean.TRUE.equals(mode.getActive())) {
                throw new BusinessException(WashModeService.MODE_UNAVAILABLE, "prices[" + i + "].washModeId");
            }
            if (wanted.put(mode.getId(), line.price()) != null) {
                throw new BusinessException("Hình thức giặt \"" + mode.getName() + "\" bị chọn trùng",
                        "prices[" + i + "].washModeId");
            }
        }
        service.getPrices().removeIf(p -> Boolean.TRUE.equals(p.getWashMode().getActive())
                && !wanted.containsKey(p.getWashMode().getId()));
        wanted.forEach((modeId, price) -> service.getPrices().stream()
                .filter(p -> p.getWashMode().getId().equals(modeId))
                .findFirst()
                .ifPresentOrElse(p -> p.setPrice(price), () -> service.getPrices().add(
                        ServiceWashModePrice.builder().service(service).washMode(modes.get(modeId)).price(price).build())));
        if (service.getPrices().isEmpty()) {
            throw new BusinessException("Dịch vụ phải có ít nhất một hình thức giặt", "prices");
        }
    }

    private void applyStatus(LaundryService service, ServiceStatus status) {
        if (status == ServiceStatus.INACTIVE) {
            service.deactivate();
        } else if (status == ServiceStatus.ACTIVE) {
            service.activate();
        }
    }

    /** Ghi lịch sử giá (BR-C02). PRICE_CHANGE bắt buộc lý do: thiếu thì AuditLogService ném lỗi và transaction rollback cả phần sửa. */
    private void logPriceChange(LaundryService service, ServicePriceSnapshot before, String reason) {
        ServicePriceSnapshot.Diff diff = before.diff(ServicePriceSnapshot.of(service));
        if (diff.isEmpty()) return;
        String cleanReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        // Chỉ thêm giá mới (giá cũ chưa có) thì không đòi lý do: ghi lý do hệ thống
        if (cleanReason == null && diff.oldData().values().stream().allMatch(Objects::isNull)) {
            cleanReason = "Thêm giá cho hình thức mới";
        }
        auditLogService.log(new AuditEntry(
                AuditAction.PRICE_CHANGE, EntityType.SERVICE, service.getId(),
                diff.oldData(), diff.newData(), cleanReason));
    }
}
