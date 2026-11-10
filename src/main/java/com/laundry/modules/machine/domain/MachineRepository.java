package com.laundry.modules.machine.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    @Query("""
        SELECT m FROM Machine m
        WHERE (:type IS NULL OR m.type = :type)
          AND (:status IS NULL OR m.status = :status)
        ORDER BY m.type, m.machineCode
    """)
    List<Machine> search(@Param("type") MachineType type, @Param("status") MachineStatus status);

    /** Số thứ tự lớn nhất đã dùng của một loại máy (phần số sau tiền tố MG / MS). */
    @Query("""
        SELECT COALESCE(MAX(CAST(SUBSTRING(m.machineCode, 3) AS Integer)), 0)
        FROM Machine m WHERE m.type = :type
    """)
    int maxCodeNumber(@Param("type") MachineType type);
}
