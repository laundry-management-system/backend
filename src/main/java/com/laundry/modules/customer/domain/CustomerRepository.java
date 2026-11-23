package com.laundry.modules.customer.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByCustomerCode(String customerCode);

    boolean existsByPhone(String phone);

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("SELECT c FROM Customer c WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.phone LIKE CONCAT('%', :search, '%') OR " +
            "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:pendingOnly = false OR EXISTS (SELECT 1 FROM CustomerRegistration r " +
            "WHERE r.customerId = c.id AND r.status = com.laundry.modules.customer.domain.RegistrationStatus.PENDING))")
    Page<Customer> searchCustomers(@Param("search") String search, @Param("status") CustomerStatus status,
                                   @Param("pendingOnly") boolean pendingOnly, Pageable pageable);

    // shortcut: đọc thẳng bảng orders để khỏi phụ thuộc vòng sang module order; chuyển sang API của module order nếu bảng đổi
    @Query(value = "SELECT EXISTS (SELECT 1 FROM orders WHERE customer_id = :id)", nativeQuery = true)
    boolean hasOrders(@Param("id") Long id);

    Optional<Customer> findTopByOrderByIdDesc();
}
