package com.laundry.modules.service.application;

import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.service.application.dto.CreateServiceRequest;
import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.application.dto.UpdateServiceRequest;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.ServiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LaundryServiceService {

    private final LaundryServiceRepository serviceRepository;

    public List<ServiceResponse> getAllServices(Boolean activeOnly) {
        List<LaundryService> list = Boolean.TRUE.equals(activeOnly)
                ? serviceRepository.findByStatusOrderByServiceCodeAsc(ServiceStatus.ACTIVE)
                : serviceRepository.findAllByOrderByServiceCodeAsc();

        return list.stream().map(ServiceResponse::from).toList();
    }

    public ServiceResponse getServiceById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceResponse::from)
                .orElseThrow(() -> new NotFoundException("Service not found with ID: " + id));
    }

    public ServiceResponse getServiceByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return serviceRepository.findByServiceCode(cleanCode)
                .map(ServiceResponse::from)
                .orElseThrow(() -> new NotFoundException("Service not found with code: " + cleanCode));
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
                .orElseThrow(() -> new NotFoundException("Service not found with ID: " + id));

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

        this.serviceRepository.save(service);

        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse changeStatus(Long id, ServiceStatus status) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found with ID: " + id));

        if (status == ServiceStatus.INACTIVE) {
            service.deactivate();
        } else if (status == ServiceStatus.ACTIVE) {
            service.activate();
        }

        LaundryService saved = serviceRepository.save(service);
        return ServiceResponse.from(saved);
    }
}
