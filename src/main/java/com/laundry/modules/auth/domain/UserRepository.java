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

    @Query(
        """
        select u from User u
            where (:status is null or u.status = :status) and
                (   :keyword is null or
                    u.employeeCode like concat('%', :keyword, '%') or
                    u.fullName like concat('%', :keyword, '%') or
                    u.phone like concat('%', :keyword, '%') or
                    u.email like concat('%', :keyword, '%')
                )
        """
      )
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
