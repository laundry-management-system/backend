package com.laundry.modules.order.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, CodeSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CodeSequence c WHERE c.seqType = :seqType AND c.seqDate = :seqDate")
    Optional<CodeSequence> findBySeqTypeAndSeqDateForUpdate(
            @Param("seqType") String seqType,
            @Param("seqDate") LocalDate seqDate
    );
}

