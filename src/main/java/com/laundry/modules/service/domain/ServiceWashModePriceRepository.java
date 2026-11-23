package com.laundry.modules.service.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceWashModePriceRepository extends JpaRepository<ServiceWashModePrice, Long> {

    List<ServiceWashModePrice> findByWashModeId(Long washModeId);

    List<ServiceWashModePrice> findByServiceId(Long serviceId);
}
