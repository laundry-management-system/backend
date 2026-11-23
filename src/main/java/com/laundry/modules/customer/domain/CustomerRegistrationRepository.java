package com.laundry.modules.customer.domain;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRegistrationRepository extends JpaRepository<CustomerRegistration, Long> {

    Page<CustomerRegistration> findByStatusOrderByCreatedAtAscIdAsc(RegistrationStatus status, Pageable pageable);

    long countByStatus(RegistrationStatus status);

    List<CustomerRegistration> findByStatusAndCustomerIdIn(RegistrationStatus status, Collection<Long> customerIds);

    Optional<CustomerRegistration> findByPhoneAndStatus(String phone, RegistrationStatus status);

    Optional<CustomerRegistration> findByEmailIgnoreCaseAndStatus(String email, RegistrationStatus status);

    boolean existsByPhoneAndStatus(String phone, RegistrationStatus status);

    boolean existsByEmailIgnoreCaseAndStatus(String email, RegistrationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerRegistration r where r.id = :id")
    Optional<CustomerRegistration> findByIdForUpdate(@Param("id") Long id);
}
