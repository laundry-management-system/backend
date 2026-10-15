package com.laundry.modules.auth.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    @Query(value = "SELECT nextval('employee_code_seq')", nativeQuery = true)
    Long nextEmployeeSeq();
}
