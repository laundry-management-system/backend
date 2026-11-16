package com.laundry.modules.shift.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByStatus(ShiftStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shift s where s.status = com.laundry.modules.shift.domain.ShiftStatus.OPEN")
    Optional<Shift> findOpenForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Shift> findWithLockById(Long id);

    /** Ca kết gần nhất: tiền để lại két của ca này là số gợi ý tiền đầu ca (BR-K07). */
    Optional<Shift> findFirstByClosedAtNotNullOrderByClosedAtDesc();

    long countByOpenedAtGreaterThanEqualAndOpenedAtLessThan(Instant from, Instant to);

    Page<Shift> findByOpenedAtGreaterThanEqualAndOpenedAtLessThan(Instant from, Instant to, Pageable pageable);

    Page<Shift> findByOpenedAtGreaterThanEqualAndOpenedAtLessThanAndStatus(Instant from, Instant to,
                                                                           ShiftStatus status, Pageable pageable);

    List<Shift> findByOpenedAtGreaterThanEqualAndOpenedAtLessThanAndStatusIn(Instant from, Instant to,
                                                                             Collection<ShiftStatus> statuses);

    long countByStatus(ShiftStatus status);
}
