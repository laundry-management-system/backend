package com.laundry.modules.machine.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    Optional<Machine> findByMachineCodeIgnoreCase(String machineCode);

    boolean existsByMachineCodeIgnoreCase(String machineCode);

    long countByType(MachineType type);

    boolean existsBySerialNumber(String serialNumber);

    @Query("SELECT m.machineCode FROM Machine m WHERE UPPER(m.machineCode) LIKE CONCAT(UPPER(:prefix), '%')")
    List<String> findCodesStartingWith(@Param("prefix") String prefix);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Long id);

    @Query("SELECT m FROM Machine m WHERE " +
            "(:type IS NULL OR m.type = :type) AND " +
            "(:status IS NULL OR m.status = :status) " +
            "ORDER BY m.machineCode ASC")
    List<Machine> findMachines(
            @Param("type") MachineType type,
            @Param("status") MachineStatus status
    );
}
