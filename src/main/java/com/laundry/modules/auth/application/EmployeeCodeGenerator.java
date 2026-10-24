package com.laundry.modules.auth.application;

import com.laundry.modules.auth.domain.UserRepository;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

/** Sinh mã nhân viên NV01, NV02... từ sequence employee_code_seq của Postgres. */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeCodeGenerator {

    static String PREFIX = "NV";

    UserRepository userRepository;

    public String next() {
        long number = Objects.requireNonNull(userRepository.nextEmployeeSeq());
        return PREFIX + "%02d".formatted(number);
    }
}