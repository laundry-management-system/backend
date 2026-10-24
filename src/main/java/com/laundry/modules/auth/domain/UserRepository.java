package com.laundry.modules.auth.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("Select u.id from User u where u.username = :username")
    Optional<Long> findIdByUsername(String username);

    @Query("""
    SELECT u FROM User u
    WHERE (:status IS NULL OR u.status = :status)
      AND (:keyword IS NULL
           OR LOWER(u.employeeCode) LIKE CAST(:keyword AS string) ESCAPE '\\'
           OR LOWER(u.fullName)     LIKE CAST(:keyword AS string) ESCAPE '\\'
           OR LOWER(u.phone)        LIKE CAST(:keyword AS string) ESCAPE '\\'
           OR LOWER(u.email)        LIKE CAST(:keyword AS string) ESCAPE '\\')
    """)
    Page<User> findAllByParam(@Param("status") Status status,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);

    @Query(value = "SELECT nextval('employee_code_seq')", nativeQuery = true)
    Long nextEmployeeSeq();

    @Query("SELECT u.username FROM User u WHERE u.username LIKE :prefix")
    List<String> findUsernamesStartingWith(@Param("prefix") String prefix);
}
