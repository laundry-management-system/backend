package com.laundry.modules.auth.application;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.NotFoundException;
import com.laundry.modules.auth.application.dto.*;
import com.laundry.modules.auth.domain.Role;
import com.laundry.modules.auth.domain.Status;
import com.laundry.modules.auth.domain.User;
import com.laundry.modules.auth.domain.UserRepository;
import com.laundry.modules.log.application.AuditLogService;
import com.laundry.modules.log.application.dto.AuditEntry;
import com.laundry.modules.log.domain.AuditAction;
import com.laundry.modules.log.domain.EntityType;
import java.util.HashMap;
import java.util.Locale;
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
        String email = createUserRequest.email().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByPhone(createUserRequest.phone())){
            throw new BusinessException("Số điện thoại đã được đăng ký");
        }

        if(userRepository.existsByEmail(createUserRequest.email())){
            throw new BusinessException("Email đã được đăng ký");
        }

        User saved;
        try{
            String passwordHash = passwordEncoder.encode(UUID.randomUUID().toString());
            String username = usernameGenerator.generate(createUserRequest.fullName());

            User user = User.builder()
                  .employeeCode(employeeCodeGenerator.next())
                  .fullName(createUserRequest.fullName())
                  .phone(createUserRequest.phone())
                  .email(email)
                  .username(username)
                  .passwordHash(passwordHash)
                  .role(createUserRequest.role())
                  .status(Status.PENDING)
                  .hiredDate(createUserRequest.hiredDate())
                  .build();

            saved = userRepository.saveAndFlush(user);
        }catch(DataIntegrityViolationException e){
            throw new BusinessException("Email hoặc số điện thoại đã tồn tại");
        }

        passwordSetupService.sendSetupMail(saved);

        Map<String, Object> newData = userToJson(saved);
        audit(AuditAction.ACCOUNT_MANAGE, saved.getId(), null, newData);

        return UserResponse.from(saved);
    }

    private Map<String, Object> userToJson(User user){
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
            Map<String, Object> oldData,
            Map<String, Object> newData){
        auditLogService.log(new AuditEntry(
                auditAction,
                EntityType.USER,
                id,
                oldData,
                newData,
                null
        ));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest){
        User user = getUserOrThrow(userId);
        Map<String, Object> oldData = userToJson(user);

        if(!updateUserRequest.phone().equals(user.getPhone()) &&
                userRepository.existsByPhone(updateUserRequest.phone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        user.updateProfile(
                updateUserRequest.fullName(),
                updateUserRequest.phone(),
                updateUserRequest.hiredDate()
        );

        try{
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
          throw new BusinessException("Số điện thoại đã tồn tại");
        }

        Map<String, Object> newData = userToJson(user);

        audit(AuditAction.ACCOUNT_MANAGE, user.getId(), oldData, newData);

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(String username, UpdateMyProfileRequest request){
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        Map<String, Object> oldData = userToJson(user);

        if(!request.phone().equals(user.getPhone()) &&
                userRepository.existsByPhone(request.phone())) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        user.updateProfile(
                request.fullName(),
                request.phone()
        );

        try{
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Số điện thoại đã tồn tại");
        }

        Map<String, Object> newData = userToJson(user);
        audit(AuditAction.ACCOUNT_MANAGE, user.getId(), oldData, newData);

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId){
        User user = getUserOrThrow(userId);
        Map<String, Object> oldData = userToJson(user);

        if(user.getRole() == Role.ADMIN){
                throw new BusinessException("Tài khoản ADMIN không thể vô hiệu hóa");
        }

        user.disable();
        userRepository.save(user);

        Map<String, Object> newData = userToJson(user);
        audit(AuditAction.ACCOUNT_MANAGE, user.getId(), oldData, newData);
    }

    @Transactional
    public void enableUser(Long userId){
        User user = getUserOrThrow(userId);
        Map<String, Object> oldData = userToJson(user);

        user.enable();
        userRepository.save(user);

        Map<String, Object> newData = userToJson(user);
        audit(AuditAction.ACCOUNT_MANAGE, user.getId(), oldData, newData);
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
        return userRepository.findAllByParam(status, toLikePattern(keyword), pageable)
                .map(UserResponse::from);
    }

    private static String toLikePattern(String keyword){
        if(keyword == null || keyword.isBlank()) return null;

        String escaped = keyword.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")   // escape trước, để không escape lại các ký tự bên dưới
                .replace("%", "\\%")
                .replace("_", "\\_");

        return "%" + escaped + "%";
    }

    @Transactional
    public void resend(Long userId){
        User user = getUserOrThrow(userId);

        user.requirePending();

        passwordSetupService.sendSetupMail(user);
    }

    @Transactional
    public void setupPassword(String token, SetupPasswordRequest request){
        Long userId = passwordSetupService.consumeToken(token);
        User user = getUserOrThrow(userId);

        user.setupPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        audit(AuditAction.SET_UP_PASSWORD, user.getId(), null, null);
    }

    User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Người dùng", userId));
    }
}
