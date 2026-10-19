package com.laundry.modules.auth.application.dto;

import com.laundry.modules.auth.domain.Role;
import com.laundry.modules.auth.domain.Status;
import com.laundry.modules.auth.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Thông tin nhân viên")
public record UserResponse(
        Long id,
        String employeeCode,
        String fullName,
        String phone,
        String email,
        String username,
        Role role,
        Status status,
        LocalDate hiredDate
) {
    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.getHiredDate()
        );
    }
}
