package com.laundry.modules.voucher.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        SELECT v FROM Voucher v
        WHERE (:status IS NULL OR v.status = :status)
          AND (:search IS NULL OR LOWER(v.code) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%')
                               OR LOWER(v.description) LIKE CONCAT('%', LOWER(CAST(:search AS string)), '%'))
    """)
    Page<Voucher> search(@Param("search") String search, @Param("status") VoucherStatus status, Pageable pageable);

    /** Giữ một lượt; trả 0 nếu đã hết lượt. Atomic nên hai quầy áp lượt cuối cùng lúc chỉ một bên thành công. */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 WHERE v.id = :id AND v.usedCount < v.maxUses")
    int reserve(@Param("id") Long id);

    /** Trả một lượt (hủy đơn, đổi/bỏ mã, Admin sửa giá). */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount - 1 WHERE v.id = :id AND v.usedCount > 0")
    int release(@Param("id") Long id);
}
