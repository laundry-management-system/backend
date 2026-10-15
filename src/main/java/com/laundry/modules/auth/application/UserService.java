package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.auth.application.dto.CreateUserRequest;
import com.laundry.modules.auth.application.dto.UserResponse;
import com.laundry.modules.auth.domain.Role;
import com.laundry.modules.auth.domain.Status;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    EmployeeCodeGenerator employeeCodeGenerator;

    @Transactional
    public UserResponse createUser(CreateUserRequest createUserRequest){
        try{
            Instant now = Instant.now();
            String passwordHash = passwordEncoder.encode(createUserRequest.password());

            User user = User.builder()
                    .employeeCode(employeeCodeGenerator.next())
                    .fullName(createUserRequest.fullName())
                    .phone(createUserRequest.phone())
                    .username(createUserRequest.username())
                    .passwordHash(passwordHash)
                    .role(Role.STAFF)
                    .status(Status.ACTIVE)
                    .hiredDate(now)
                    .build();

            userRepository.save(user);

            return UserResponse.from(user);
        }catch(DataIntegrityViolationException e){
            throw new BusinessException("Tên đăng nhập hoặc số điện thoại đã tồn tại");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        return UserResponse.from(user);
    }
}
