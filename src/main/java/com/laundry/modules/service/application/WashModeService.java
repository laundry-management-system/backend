package com.laundry.modules.service.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import com.laundry.modules.service.application.dto.WashModeDetailResponse;
import com.laundry.modules.service.application.dto.WashModeRequest;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.ServicePriceSnapshot;
import com.laundry.modules.service.domain.ServiceWashModePrice;
import com.laundry.modules.service.domain.ServiceWashModePriceRepository;
import com.laundry.modules.service.domain.WashMode;
import com.laundry.modules.service.domain.WashModeRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WashModeService {

    static final String MODE_UNAVAILABLE = "Hình thức giặt không còn áp dụng, vui lòng chọn lại";

    private final WashModeRepository washModeRepository;
    private final ServiceWashModePriceRepository priceRepository;
    private final LaundryServiceRepository serviceRepository;
    private final AuditLogService auditLogService;

    public WashModeDetailResponse get(Long id) {
        return detail(find(id));
    }

    @Transactional
    public WashModeDetailResponse create(WashModeRequest request) {
        return save(WashMode.builder().build(), request);
    }

    @Transactional
    public WashModeDetailResponse update(Long id, WashModeRequest request) {
        return save(find(id), request);
    }

    private WashMode find(Long id) {
        return washModeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hình thức giặt"));
    }

    private WashModeDetailResponse detail(WashMode mode) {
        List<ServiceWashModePrice> links = priceRepository.findByWashModeId(mode.getId()).stream()
                .sorted(Comparator.comparing(l -> l.getService().getServiceCode()))
                .toList();
        return WashModeDetailResponse.from(mode, links);
    }

    /** Lưu hình thức và thay toàn bộ liên kết giá trong một transaction (FR-001, FR-001a, FR-001c, FR-001d, FR-003). */
    private WashModeDetailResponse save(WashMode mode, WashModeRequest request) {
        String name = request.name().trim();
        boolean nameTaken = mode.getId() == null
                ? washModeRepository.existsByNameIgnoreCase(name)
                : washModeRepository.existsByNameIgnoreCaseAndIdNot(name, mode.getId());
        if (nameTaken) {
            throw new BusinessException("Tên hình thức giặt đã tồn tại", "name");
        }
        boolean wasDefault = Boolean.TRUE.equals(mode.getIsDefault());
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (wasDefault && !makeDefault) {
            throw new BusinessException("Phải có một hình thức mặc định; hãy đặt hình thức khác làm mặc định", "isDefault");
        }
        if (wasDefault && Boolean.FALSE.equals(request.active())) {
            throw new BusinessException("Không thể ngừng hình thức mặc định", "active");
        }
        if (makeDefault && !wasDefault) {
            washModeRepository.findByIsDefaultTrue().ifPresent(old -> {
                old.setIsDefault(false);
                washModeRepository.saveAndFlush(old);
            });
        }
        mode.setName(name);
        mode.setCompletionHours(request.completionHours().intValueExact());
        mode.setActive(makeDefault || !Boolean.FALSE.equals(request.active()));
        mode.setIsDefault(makeDefault);
        boolean isNew = mode.getCode() == null;
        if (isNew) {
            // cột code NOT NULL nhưng mã thật cần id: giữ chỗ như serviceCode trong LaundryServiceService
            mode.setCode("TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        }
        WashMode saved = washModeRepository.saveAndFlush(mode);
        if (isNew) {
            saved.setCode(String.format("HT%02d", saved.getId()));
        }

        replaceLinks(saved, request.services() != null ? request.services() : List.of());
        priceRepository.flush();
        return detail(saved);
    }

    private void replaceLinks(WashMode mode, List<WashModeRequest.ServiceLine> lines) {
        Map<Long, LaundryService> services = serviceRepository
                .findAllById(lines.stream().map(WashModeRequest.ServiceLine::serviceId).toList()).stream()
                .collect(Collectors.toMap(LaundryService::getId, Function.identity()));
        Map<Long, BigDecimal> wanted = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            WashModeRequest.ServiceLine line = lines.get(i);
            LaundryService service = services.get(line.serviceId());
            if (service == null) {
                throw new BusinessException("Dịch vụ không tồn tại", "services[" + i + "].serviceId");
            }
            if (wanted.put(service.getId(), line.price()) != null) {
                throw new BusinessException("Dịch vụ \"" + service.getName() + "\" bị chọn trùng",
                        "services[" + i + "].serviceId");
            }
        }
        // Dịch vụ đang liên kết nhưng không còn trong request: bỏ liên kết
        priceRepository.findByWashModeId(mode.getId()).forEach(l -> services.putIfAbsent(l.getService().getId(), l.getService()));

        Map<Long, ServicePriceSnapshot> before = new HashMap<>();
        services.values().forEach(s -> before.put(s.getId(), ServicePriceSnapshot.of(s)));
        for (LaundryService service : services.values()) {
            BigDecimal price = wanted.get(service.getId());
            ServiceWashModePrice link = service.getPrices().stream()
                    .filter(p -> p.getWashMode().getId().equals(mode.getId()))
                    .findFirst().orElse(null);
            if (price == null) {
                service.getPrices().remove(link);
                if (service.getPrices().isEmpty()) {
                    throw new BusinessException("Dịch vụ \"" + service.getName()
                            + "\" phải còn ít nhất một hình thức giặt", "services");
                }
            } else if (link != null) {
                link.setPrice(price);
            } else {
                service.getPrices().add(ServiceWashModePrice.builder().service(service).washMode(mode).price(price).build());
            }
            ServicePriceSnapshot.Diff diff = before.get(service.getId()).diff(ServicePriceSnapshot.of(service));
            if (!diff.isEmpty()) {
                // PRICE_CHANGE bắt buộc lý do mà form hình thức không có ô lý do: ghi lý do hệ thống
                auditLogService.log(new AuditEntry(AuditAction.PRICE_CHANGE, EntityType.SERVICE, service.getId(),
                        diff.oldData(), diff.newData(), "Sửa giá ở hình thức giặt \"" + mode.getName() + "\""));
            }
        }
    }

    public List<WashMode> listAll() {
        return washModeRepository.findAll().stream().sorted(WashMode.DISPLAY_ORDER).toList();
    }

    public List<WashMode> listActive() {
        return listAll().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).toList();
    }

    public WashMode getDefault() {
        return washModeRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new BusinessException("Chưa có hình thức giặt mặc định"));
    }

    /**
     * Đơn giá theo hình thức cho từng dịch vụ (serviceId -> giá). Dịch vụ phải liên kết hình thức này
     * và hình thức mặc định hiện tại (FR-010, FR-011).
     */
    public Map<Long, BigDecimal> requirePrices(Long washModeId, List<Long> serviceIds, boolean allowStoppedMode) {
        WashMode mode = washModeId == null ? null : washModeRepository.findById(washModeId).orElse(null);
        if (mode == null || (!allowStoppedMode && !Boolean.TRUE.equals(mode.getActive()))) {
            throw new BusinessException(MODE_UNAVAILABLE, "washModeId");
        }
        Map<Long, BigDecimal> modePrices = pricesOf(mode.getId());
        Set<Long> defaultLinked = pricesOf(getDefault().getId()).keySet();
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < serviceIds.size(); i++) {
            Long serviceId = serviceIds.get(i);
            BigDecimal price = modePrices.get(serviceId);
            if (price == null || !defaultLinked.contains(serviceId)) {
                String name = serviceRepository.findById(serviceId).map(LaundryService::getName).orElse("#" + serviceId);
                throw new BusinessException("Dịch vụ \"" + name + "\" không áp dụng cho hình thức "
                        + mode.getName(), "items[" + i + "].serviceId");
            }
            result.put(serviceId, price);
        }
        return result;
    }

    private Map<Long, BigDecimal> pricesOf(Long washModeId) {
        return priceRepository.findByWashModeId(washModeId).stream()
                .collect(Collectors.toMap(p -> p.getService().getId(), ServiceWashModePrice::getPrice));
    }
}
