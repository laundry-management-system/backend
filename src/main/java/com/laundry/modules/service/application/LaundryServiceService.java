package com.laundry.modules.service.application;

import com.laundry.common.exception.BusinessException;
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

@Service
@RequiredArgsConstructor
public class LaundryServiceService {

    private final LaundryServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllServices(Boolean activeOnly) {
        List<LaundryService> list = Boolean.TRUE.equals(activeOnly)
                ? serviceRepository.findByStatusOrderByServiceCodeAsc(ServiceStatus.ACTIVE)
                : serviceRepository.findAllByOrderByServiceCodeAsc();

        return list.stream().map(ServiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        return serviceRepository.findById(id)
                .map(ServiceResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ với ID: " + id));
    }

    @Transactional(readOnly = true)
    public ServiceResponse getServiceByCode(String code) {
        String cleanCode = code != null ? code.trim() : "";
        return serviceRepository.findByServiceCode(cleanCode)
                .map(ServiceResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ với mã: " + cleanCode));
    }

    @Transactional
    public ServiceResponse createService(CreateServiceRequest request) {
        String code = request.serviceCode() != null && !request.serviceCode().isBlank()
                ? request.serviceCode().trim().toUpperCase()
                : generateServiceCode();

        if (serviceRepository.existsByServiceCode(code)) {
            throw new BusinessException("Mã dịch vụ '" + code + "' đã tồn tại trong bảng giá");
        }

        LaundryService service = LaundryService.builder()
                .serviceCode(code)
                .name(request.name().trim())
                .unit(request.unit())
                .currentPrice(request.currentPrice())
                .standardHours(request.standardHours())
                .expressSupported(Boolean.TRUE.equals(request.expressSupported()))
                .expressSurcharge(request.expressSurcharge())
                .expressHours(request.expressHours())
                .status(ServiceStatus.ACTIVE)
                .build();

        service.validateBusinessRules();

        LaundryService saved = serviceRepository.save(service);
        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse updateService(Long id, UpdateServiceRequest request) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ với ID: " + id));

        service.updateDetails(
                request.name(),
                request.unit(),
                request.currentPrice(),
                request.standardHours(),
                request.expressSupported(),
                request.expressSurcharge(),
                request.expressHours()
        );

        LaundryService saved = serviceRepository.save(service);
        return ServiceResponse.from(saved);
    }

    @Transactional
    public ServiceResponse changeStatus(Long id, ServiceStatus status) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ với ID: " + id));

        if (status == ServiceStatus.INACTIVE) {
            service.deactivate();
        } else if (status == ServiceStatus.ACTIVE) {
            service.activate();
        }

        LaundryService saved = serviceRepository.save(service);
        return ServiceResponse.from(saved);
    }

    private String generateServiceCode() {
        long nextId = serviceRepository.findTopByOrderByIdDesc()
                .map(s -> s.getId() + 1)
                .orElse(1L);

        String candidate = String.format("DV%02d", nextId);
        long counter = nextId;
        while (serviceRepository.existsByServiceCode(candidate)) {
            counter++;
            candidate = String.format("DV%02d", counter);
        }
        return candidate;
    }
}
