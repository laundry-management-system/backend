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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LaundryServiceService {

    private final LaundryServiceRepository serviceRepository;

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
                .status(ServiceStatus.ACTIVE)
                .build();

        service.validateBusinessRules();

        LaundryService saved = serviceRepository.saveAndFlush(service);
        saved.setServiceCode(String.format("DV%02d",saved.getId()));

        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse updateService(Long id, UpdateServiceRequest request) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));

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

        service.validateBusinessRules();

        if (request.status() != null && request.status() != service.getStatus()) {
            applyStatus(service, request.status());
        }

        this.serviceRepository.save(service);

        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse changeStatus(Long id, ServiceStatus status) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Dịch vụ", id));

        applyStatus(service, status);

        LaundryService saved = serviceRepository.save(service);
        return ServiceResponse.from(saved);
    }

    private void applyStatus(LaundryService service, ServiceStatus status) {
        if (status == ServiceStatus.INACTIVE) {
            service.deactivate();
        } else if (status == ServiceStatus.ACTIVE) {
            service.activate();
        }
    }
}
