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
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    AuditLogService auditLogService;

    @Transactional
    public UserResponse createUser(CreateUserRequest createUserRequest){
        if(userRepository.existsByPhone(createUserRequest.phone())){
            throw new BusinessException("Số điện thoại đã được đăng ký");
        }

        if(userRepository.existsByEmail(createUserRequest.email())){
            throw new BusinessException("Email đã được đăng ký");
        }

        try{
            String passwordHash = passwordEncoder.encode(UUID.randomUUID().toString());
            String username = usernameGenerator.generate(createUserRequest.fullName());

            User user = User.builder()
                  .employeeCode(employeeCodeGenerator.next())
                  .fullName(createUserRequest.fullName())
                  .phone(createUserRequest.phone())
                  .email(createUserRequest.email())
                  .username(username)
                  .passwordHash(passwordHash)
                  .role(createUserRequest.role())
                  .status(Status.INACTIVE)
                  .hiredDate(createUserRequest.hiredDate())
                  .build();

            userRepository.save(user);

            passwordSetupService.sendSetupMail(user);
            audit(AuditAction.ACCOUNT_MANAGE, user.getId(), null, user);

            return UserResponse.from(user);
        }catch(DataIntegrityViolationException e){
            throw new BusinessException("Email hoặc số điện thoại đã tồn tại");
        }
    }

    private Map<String, Object> userToJson(User user){
        if(user == null) return Collections.emptyMap();

        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("fullName", user.getFullName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("username", user.getUsername());
        map.put("role", user.getRole());
        map.put("status", user.getStatus());
        map.put("hiredDate", user.getHiredDate());

        return map;
    }

    private void audit(
            AuditAction auditAction,
            Long id,
            User oldData,
            User newData){

        Map<String, Object> oldD = userToJson(oldData);
        Map<String, Object> newD = userToJson(newData);

        auditLogService.log(new AuditEntry(
                auditAction,
                EntityType.USER,
                id,
                oldD,
                newD,
                null
        ));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest){
        User oldData = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));

        if(!oldData.isActive()) throw new BusinessException("Người dùng đã bị vô hiệu hóa");

        if(!updateUserRequest.phone().equals(oldData.getPhone()) &&
                userRepository.existsByPhone(updateUserRequest.phone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        try{
            User newData = oldData.updateProfile(
                    updateUserRequest.fullName(),
                    updateUserRequest.phone(),
                    updateUserRequest.hiredDate()
            );

            userRepository.save(newData);
            audit(AuditAction.ACCOUNT_MANAGE, oldData.getId(), oldData, newData);

            return UserResponse.from(newData);
        } catch (DataIntegrityViolationException e) {
          throw new BusinessException("Số điện thoại đã tồn tại");
        }

    }

    @Transactional
    public UserResponse updateMyProfile(String username, UpdateUserRequest updateUserRequest){
        User oldData = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        if(!oldData.isActive()) throw new BusinessException("Người dùng đã bị vô hiệu hóa");

        if(!updateUserRequest.phone().equals(oldData.getPhone()) &&
                userRepository.existsByPhone(updateUserRequest.phone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        try{
            User newData = oldData.updateProfile(
                    updateUserRequest.fullName(),
                    updateUserRequest.phone(),
                    updateUserRequest.hiredDate()
            );

            userRepository.save(newData);
        audit(AuditAction.ACCOUNT_MANAGE, oldData.getId(), oldData, newData);

            return UserResponse.from(newData);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }
    }

    @Transactional
    public void deleteUser(Long userId){
        User oldData = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));
        if(oldData.getRole() == Role.ADMIN){
                throw new BusinessException("Tài khoản ADMIN không thể vô hiệu hóa");
        }

        if(!oldData.isActive()){
            throw new BusinessException("Người dùng đã bị vô hiệu hóa");
        }
        User newData = oldData.disable();

        userRepository.save(newData);
        audit(AuditAction.ACCOUNT_MANAGE, oldData.getId(), oldData, newData);
    }

    @Transactional
    public void enableUser(Long userId){
        User oldData = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));
        if(!oldData.isActive()){
            User newData = oldData.enable();
            userRepository.save(newData);

            audit(AuditAction.ACCOUNT_MANAGE, oldData.getId(), oldData, newData);
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

        audit(AuditAction.SET_UP_PASSWORD, user.getId(), null, null);
    }
}
