package com.laundry.modules.service;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.service.application.LaundryServiceService;
import com.laundry.modules.service.application.dto.CreateServiceRequest;
import com.laundry.modules.service.application.dto.ServiceResponse;
import com.laundry.modules.service.application.dto.UpdateServiceRequest;
import com.laundry.modules.service.domain.LaundryService;
import com.laundry.modules.service.domain.LaundryServiceRepository;
import com.laundry.modules.service.domain.ServiceStatus;
import com.laundry.modules.service.domain.ServiceUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaundryServiceServiceTest {

    @Mock
    private LaundryServiceRepository serviceRepository;

    @InjectMocks
    private LaundryServiceService serviceService;

    @Test
    @DisplayName("Get all services in active price list (BM2)")
    void getAllServices_ReturnsList() {
        LaundryService s1 = LaundryService.builder()
                .serviceCode("DV01")
                .name("Wash & Dry")
                .unit(ServiceUnit.KG)
                .currentPrice(BigDecimal.valueOf(20000))
                .standardHours(24)
                .status(ServiceStatus.ACTIVE)
                .build();

        when(serviceRepository.findAllByOrderByServiceCodeAsc()).thenReturn(List.of(s1));

        List<ServiceResponse> result = serviceService.getAllServices(false);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().serviceCode()).isEqualTo("DV01");
        assertThat(result.getFirst().unitDisplayName()).isEqualTo("kg");
    }

    @Test
    @DisplayName("Create new service successfully (Admin adds to price list)")
    void createService_Success() {
        CreateServiceRequest request = new CreateServiceRequest(
                "DV01",
                "Clothes Wash & Dry",
                ServiceUnit.KG,
                BigDecimal.valueOf(20000),
                24,
                true,
                BigDecimal.valueOf(10000),
                6
        );

        when(serviceRepository.existsByServiceCode("DV01")).thenReturn(false);

        LaundryService saved = LaundryService.builder()
                .serviceCode("DV01")
                .name(request.name())
                .unit(request.unit())
                .currentPrice(request.currentPrice())
                .standardHours(request.standardHours())
                .expressSupported(request.expressSupported())
                .expressSurcharge(request.expressSurcharge())
                .expressHours(request.expressHours())
                .status(ServiceStatus.ACTIVE)
                .build();

        when(serviceRepository.save(any(LaundryService.class))).thenReturn(saved);

        ServiceResponse response = serviceService.createService(request);

        assertThat(response).isNotNull();
        assertThat(response.serviceCode()).isEqualTo("DV01");
        assertThat(response.currentPrice()).isEqualTo(BigDecimal.valueOf(20000));
        assertThat(response.expressSupported()).isTrue();
        assertThat(response.expressHours()).isEqualTo(6);
        verify(serviceRepository).save(any(LaundryService.class));
    }

    @Test
    @DisplayName("Create service fails when express hours >= standard hours (BR-C06)")
    void createService_InvalidExpressHours_ThrowsBusinessException() {
        CreateServiceRequest request = new CreateServiceRequest(
                "DV02",
                "Premium Dry Cleaning",
                ServiceUnit.PIECE,
                BigDecimal.valueOf(50000),
                24,
                true,
                BigDecimal.valueOf(20000),
                24 // expressHours equals standardHours -> violates BR-C06
        );

        when(serviceRepository.existsByServiceCode("DV02")).thenReturn(false);

        assertThatThrownBy(() -> serviceService.createService(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("BR-C06");
    }

    @Test
    @DisplayName("Create service fails when service code already exists")
    void createService_DuplicateCode_ThrowsBusinessException() {
        CreateServiceRequest request = new CreateServiceRequest(
                "DV01",
                "Duplicate Wash & Dry",
                ServiceUnit.KG,
                BigDecimal.valueOf(20000),
                24,
                false,
                null,
                null
        );

        when(serviceRepository.existsByServiceCode("DV01")).thenReturn(true);

        assertThatThrownBy(() -> serviceService.createService(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Update service details successfully")
    void updateService_Success() {
        LaundryService existing = LaundryService.builder()
                .serviceCode("DV01")
                .name("Old Wash & Dry")
                .unit(ServiceUnit.KG)
                .currentPrice(BigDecimal.valueOf(20000))
                .standardHours(24)
                .expressSupported(false)
                .status(ServiceStatus.ACTIVE)
                .build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(LaundryService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateServiceRequest updateReq = new UpdateServiceRequest(
                "Updated Wash & Dry",
                ServiceUnit.KG,
                BigDecimal.valueOf(25000),
                20,
                true,
                BigDecimal.valueOf(10000),
                5
        );

        ServiceResponse response = serviceService.updateService(1L, updateReq);

        assertThat(response.name()).isEqualTo("Updated Wash & Dry");
        assertThat(response.currentPrice()).isEqualTo(BigDecimal.valueOf(25000));
        assertThat(response.standardHours()).isEqualTo(20);
        assertThat(response.expressHours()).isEqualTo(5);
    }

    @Test
    @DisplayName("Deactivate service (change status to INACTIVE instead of delete per BR-C05)")
    void changeStatus_Deactivate_Success() {
        LaundryService existing = LaundryService.builder()
                .serviceCode("DV01")
                .name("Service to deactivate")
                .status(ServiceStatus.ACTIVE)
                .build();

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(serviceRepository.save(any(LaundryService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceResponse response = serviceService.changeStatus(1L, ServiceStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(ServiceStatus.INACTIVE);
    }

    @Test
    @DisplayName("Get service by ID fails when not found -> throws NotFoundException")
    void getServiceById_NotFound() {
        when(serviceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.getServiceById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Service not found");
    }
}
