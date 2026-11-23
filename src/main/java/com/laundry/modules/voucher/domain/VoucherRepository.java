package com.laundry.modules.voucher.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    /** Khóa dòng voucher để hai đơn áp cùng lúc không vượt số lượt tối đa */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE UPPER(v.code) = UPPER(:code)")
    Optional<Voucher> lockByCode(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> lockById(@Param("id") Long id);

    @Query("SELECT v FROM Voucher v WHERE " +
            "(:status IS NULL OR v.status = :status) AND " +
            "(:search IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) ESCAPE '\\')")
    Page<Voucher> searchVouchers(
            @Param("search") String search,
            @Param("status") VoucherStatus status,
            Pageable pageable
    );
}
