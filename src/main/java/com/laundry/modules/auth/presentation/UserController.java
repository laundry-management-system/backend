package com.laundry.modules.auth.presentation;

import com.laundry.modules.auth.application.UserService;
import com.laundry.modules.auth.application.dto.*;
import com.laundry.modules.auth.domain.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest createUserRequest) {
        return userService.createUser(createUserRequest);
    }

    @Operation(summary = "Lấy thông tin nhân viên", description = "API lấy thông tin nhân viên theo ID")
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "Lấy thông tin của bản thân", description = "API lấy thông tin của chủ sở hữu token")
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getMyProfile(Authentication authentication) {
        return userService.getMyProfile(authentication.getName());
    }

    @Operation(
      summary = "Lấy thông tin tất cả nhân viên có filter",
      description = "API lấy thông tin tất cả nhân viên lọc theo trạng thái (status), từ khóa (mã nhân viên, họ tên, số điện thoại, email), có phân trang"
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getAllUsers(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return userService.getAllUsers(status, keyword, pageable);
    }

    @Operation(summary = "Cập nhật thông tin nhân viên", description = "API cập nhật thông tin nhân viên")
    @PatchMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(@PathVariable Long userId, @RequestBody @Valid UpdateUserRequest updateUserRequest) {
        return userService.updateUser(userId, updateUserRequest);
    }

    @Operation(summary = "Cập nhật thông tin của bản thân", description = "API cập nhật thông tin của bản thân")
    @PatchMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse updateMyInfo(@RequestBody @Valid UpdateMyProfileRequest request,
                                     Authentication authentication) {
    return userService.updateMyProfile(authentication.getName(), request);
    }

    @Operation(summary = "Xóa nhân viên", description = "API xóa nhân viên theo ID")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }

    @Operation(summary = "Kích hoạt tài khoản nhân viên", description = "API kích hoạt tài khoản nhân viên theo ID")
    @PostMapping("/{userId}/enable")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public void enableUser(@PathVariable Long userId) {
        userService.enableUser(userId);
    }

    @Operation(summary = "Gửi lại mail setup tài khoản", description = "API cho phép ADMIN gửi lại mail setup tài khoản cho nhân viên")
    @PostMapping("/{id}/resend-setup-mail")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public void resendSetupMail(@PathVariable("id") Long userId) {
        userService.resend(userId);
    }

    @Operation(summary = "Thiết lập mật khẩu", description = "API cho phép nhân viên thiết lập mật khẩu")
    @PostMapping("/setup-password")
    @ResponseStatus(HttpStatus.OK)
    public void setupPassword(@RequestParam String token, @RequestBody @Valid SetupPasswordRequest request){
        userService.setupPassword(token, request);
    }
}
