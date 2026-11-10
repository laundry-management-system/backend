package com.laundry.modules.customer.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByCustomerCode(String customerCode);

    boolean existsByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.phone LIKE CONCAT('%', :search, '%') OR " +
            "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchCustomers(@Param("search") String search, @Param("status") CustomerStatus status,
                                   Pageable pageable);

    Optional<Customer> findTopByOrderByIdDesc();
}
