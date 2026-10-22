package com.laundry.modules.service.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LaundryServiceRepository extends JpaRepository<LaundryService, Long> {

    Optional<LaundryService> findByServiceCode(String serviceCode);

    boolean existsByServiceCode(String serviceCode);

    List<LaundryService> findByStatusOrderByServiceCodeAsc(ServiceStatus status);

    // search luôn là chuỗi (rỗng = không lọc): tham số String null bị Postgres hiểu là bytea khi LIKE
    @Query("""
            SELECT s FROM LaundryService s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:search = ''
                   OR LOWER(s.serviceCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LaundryService> searchServices(@Param("search") String search,
                                        @Param("status") ServiceStatus status,
                                        Pageable pageable);

    Optional<LaundryService> findTopByOrderByIdDesc();
}
