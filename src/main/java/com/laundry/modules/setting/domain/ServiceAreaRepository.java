package com.laundry.modules.setting.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAreaRepository extends JpaRepository<ServiceArea, Long> {

    List<ServiceArea> findAllByOrderByIdAsc();

    List<ServiceArea> findByActiveTrueOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
