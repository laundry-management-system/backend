package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.auth.application.dto.CreateUserRequest;
import com.laundry.modules.auth.application.dto.SetupPasswordRequest;
import com.laundry.modules.auth.application.dto.UpdateUserRequest;
import com.laundry.modules.auth.application.dto.UserResponse;
import com.laundry.modules.auth.domain.Role;
import com.laundry.modules.auth.domain.Status;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    UsernameGenerator usernameGenerator;
    PasswordSetupService passwordSetupService;

    @Transactional
    public UserResponse createUser(CreateUserRequest createUserRequest){
        if(userRepository.existsByPhone(createUserRequest.phone())){
            throw new BusinessException("Số điện thoại đã được đăng ký");
        }

        if(userRepository.existsByEmail(createUserRequest.email())){
            throw new BusinessException("Email đã được đăng ký");
        }

        try{
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            String passwordHash = passwordEncoder.encode(UUID.randomUUID().toString());
            String username = usernameGenerator.generate(createUserRequest.fullName());

      User user =
          User.builder()
              .employeeCode(employeeCodeGenerator.next())
              .fullName(createUserRequest.fullName())
              .phone(createUserRequest.phone())
              .email(createUserRequest.email())
              .username(username)
              .passwordHash(passwordHash)
              .role(createUserRequest.role())
              .status(Status.INACTIVE)
              .hiredDate(today)
              .build();

            userRepository.save(user);

            passwordSetupService.sendSetupMail(user);

            return UserResponse.from(user);
        }catch(DataIntegrityViolationException e){
            throw new BusinessException("Email hoặc số điện thoại đã tồn tại");
        }

        // TODO(audit): ghi AuditLog ACCOUNT_MANAGE (BR-J01) - người thực hiện = Admin hiện tại, entity = user/{id}, old/new = status
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));

        if(!user.isActive()) throw new BusinessException("Người dùng đã bị vô hiệu hóa");

        if(!updateUserRequest.phone().equals(user.getPhone()) &&
                userRepository.existsByPhone(updateUserRequest.phone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

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
    public UserResponse updateMyProfile(String username, UpdateUserRequest updateUserRequest){
    User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        if(!user.isActive()) throw new BusinessException("Người dùng đã bị vô hiệu hóa");

        if(!updateUserRequest.phone().equals(user.getPhone()) &&
                userRepository.existsByPhone(updateUserRequest.phone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

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
        if(user.getRole() == Role.ADMIN){
                throw new BusinessException("Tài khoản ADMIN không thể vô hiệu hóa");
        }

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

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String username){
        return UserResponse.from(userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin người dùng")));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Status status, String keyword, Pageable pageable){
        String normalizedKeyword = keyword == null ? null: keyword.trim();
        return userRepository.findAllByParam(status, normalizedKeyword, pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public void resend(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("Người dùng", userId));
        passwordSetupService.sendSetupMail(user);
    }

    @Transactional
    public void setupPassword(String token, SetupPasswordRequest request){
        String passwordHash = passwordEncoder.encode(request.password());
        Long userId = passwordSetupService.consumeToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));

        if(user.isActive()){
            throw new BusinessException("Tài khoản đã được kích hoạt");
        }

        user.setupPassword(passwordHash);
        user.enable();
        userRepository.save(user);
    }
}
