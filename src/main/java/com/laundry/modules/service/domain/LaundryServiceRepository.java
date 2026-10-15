package com.laundry.modules.service.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaundryServiceRepository extends JpaRepository<LaundryService, Long> {

    Optional<LaundryService> findByServiceCode(String serviceCode);

    boolean existsByServiceCode(String serviceCode);

    boolean existsByServiceCodeAndIdNot(String serviceCode, Long id);

    List<LaundryService> findByStatusOrderByServiceCodeAsc(ServiceStatus status);

    List<LaundryService> findAllByOrderByServiceCodeAsc();

    Optional<LaundryService> findTopByOrderByIdDesc();
}
