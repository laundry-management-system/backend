package com.laundry.modules.auth.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    @Query(
        """
        select u from User u
            where (:status is null or u.status = :status) and
                (   :keyword is null or
                    u.employeeCode like concat('%', :keyword, '%') or
                    u.fullName like concat('%', :keyword, '%') or
                    u.phone like concat('%', :keyword, '%')
                )
        """
      )
    Page<User> findAllByParam(@Param("status") Status status,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);

    @Query(value = "SELECT nextval('employee_code_seq')", nativeQuery = true)
    Long nextEmployeeSeq();


}
