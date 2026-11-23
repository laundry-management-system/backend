package com.laundry.modules.service.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WashModeRepository extends JpaRepository<WashMode, Long> {

    Optional<WashMode> findByIsDefaultTrue();

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCase(String name);
}
