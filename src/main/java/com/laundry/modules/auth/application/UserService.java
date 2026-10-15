package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.auth.application.dto.CreateUserRequest;
import com.laundry.modules.auth.application.dto.UpdateUserRequest;
import com.laundry.modules.auth.application.dto.UserResponse;
import com.laundry.modules.auth.domain.Role;
import com.laundry.modules.auth.domain.Status;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;

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
        if(userRepository.findByUsername(createUserRequest.username()).isPresent()){
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        if(userRepository.findByPhone(createUserRequest.phone()).isPresent()){
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        try{
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            String passwordHash = passwordEncoder.encode(createUserRequest.password());

            User user = User.builder()
                    .employeeCode(employeeCodeGenerator.next())
                    .fullName(createUserRequest.fullName())
                    .phone(createUserRequest.phone())
                    .username(createUserRequest.username())
                    .passwordHash(passwordHash)
                    .role(Role.STAFF)
                    .status(Status.ACTIVE)
                    .hiredDate(today)
                    .build();

            userRepository.save(user);

            return UserResponse.from(user);
        }catch(DataIntegrityViolationException e){
            throw new BusinessException("Tên đăng nhập hoặc số điện thoại đã tồn tại");
        }

        // TODO(audit): ghi AuditLog ACCOUNT_MANAGE (BR-J01) - người thực hiện = Admin hiện tại, entity = user/{id}, old/new = status
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));

        if(!user.isActive()) throw new BusinessException("Người dùng đã bị vô hiệu hóa");

        try{
            user.updateProfile(updateUserRequest.fullName(), updateUserRequest.phone());
            userRepository.save(user);
            return UserResponse.from(user);
        } catch (DataIntegrityViolationException e) {
          throw new BusinessException("Số điện thoại đã tồn tại");
        }

        // TODO(audit): ghi AuditLog ACCOUNT_MANAGE (BR-J01) - người thực hiện = Admin hiện tại, entity = user/{id}, old/new = status
    }

    @Transactional
    public void deleteUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));

        if(!user.isActive()){
            throw new BusinessException("Người dùng đã bị vô hiệu hóa");
        }
        user.disable();

        // TODO(audit): ghi AuditLog ACCOUNT_MANAGE (BR-J01) - người thực hiện = Admin hiện tại, entity = user/{id}, old/new = status
        userRepository.save(user);
    }

    @Transactional
    public void enableUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));
        if(!user.isActive()){
            user.enable();
            // TODO(audit): ghi AuditLog ACCOUNT_MANAGE (BR-J01) - người thực hiện = Admin hiện tại, entity = user/{id}, old/new = status
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));
        return UserResponse.from(user);
    }
}
