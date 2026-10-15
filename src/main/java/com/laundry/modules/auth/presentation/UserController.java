package com.laundry.modules.auth.presentation;

import com.laundry.modules.auth.application.UserService;
import com.laundry.modules.auth.application.dto.CreateUserRequest;
import com.laundry.modules.auth.application.dto.UpdateUserRequest;
import com.laundry.modules.auth.application.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Management", description = "API quản lý nhân viên")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/users")
public class UserController {
    UserService userService;

    @Operation(summary = "Tạo nhân viên", description = "API tạo nhân viên mới")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest createUserRequest) {
        return userService.createUser(createUserRequest);
    }

    @Operation(summary = "Lấy thông tin nhân viên", description = "API lấy thông tin nhân viên theo ID")
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "Cập nhật thông tin nhân viên", description = "API cập nhật thông tin nhân viên")
    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse updateUser(@PathVariable Long userId, @RequestBody @Valid UpdateUserRequest updateUserRequest) {
        return userService.updateUser(userId, updateUserRequest);
    }

    @Operation(summary = "Cập nhật thông tin của bản thân", description = "API cập nhật thông tin của bản thân")
    @PutMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse updateMyInfo(@RequestBody @Valid UpdateUserRequest updateUserRequest,
                                     Authentication authentication) {
        return userService.updateUser(Long.valueOf(authentication.getName()), updateUserRequest);
    }

    @Operation(summary = "Xóa nhân viên", description = "API xóa nhân viên theo ID")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    @Operation(summary = "Kích hoạt tài khoản nhân viên", description = "API kích hoạt tài khoản nhân viên theo ID")
    @PostMapping("/{userId}/enable")
    @ResponseStatus(HttpStatus.OK)
    public void enableUser(@PathVariable Long userId) {
        userService.enableUser(userId);
    }
}
