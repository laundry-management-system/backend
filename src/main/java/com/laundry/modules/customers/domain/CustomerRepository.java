package com.laundry.modules.customers.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByPhone(String phone);

    List<Customer> findByFullNameContainingIgnoreCaseOrPhoneContaining(String fullName, String phone);
}
