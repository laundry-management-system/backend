package com.laundry.modules.customer.presentation;

import com.laundry.common.security.RefreshTokenCookie;
import com.laundry.modules.auth.application.dto.AuthDTO;
import com.laundry.modules.auth.application.dto.AuthenticationResponse;
import com.laundry.modules.customer.application.CustomerAuthRateLimiter;
import com.laundry.modules.customer.application.CustomerAuthService;
import com.laundry.modules.customer.application.CustomerPasswordResetService;
import com.laundry.modules.customer.application.dto.CustomerLoginRequest;
import com.laundry.modules.customer.application.dto.CustomerRegisterRequest;
import com.laundry.modules.customer.application.dto.CustomerResetPasswordRequest;
import com.laundry.modules.customer.application.dto.ForgotPasswordRequest;
import com.laundry.modules.customer.application.dto.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Auth", description = "Đăng ký, đăng nhập và đặt lại mật khẩu của khách")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer-auth")
public class CustomerAuthController {

    private final CustomerAuthService authService;
    private final CustomerPasswordResetService passwordResetService;
    private final CustomerAuthRateLimiter rateLimiter;
    private final RefreshTokenCookie refreshTokenCookie;

    @Operation(summary = "Đăng ký tài khoản khách", description = "Tạo yêu cầu chờ Admin kích hoạt")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody @Valid CustomerRegisterRequest request, HttpServletRequest http) {
        rateLimiter.check("register", CustomerAuthRateLimiter.clientKey(http), 5, Duration.ofHours(1));
        authService.register(request);
        return new RegisterResponse("PENDING");
    }

    @Operation(summary = "Đăng nhập khách bằng email hoặc số điện thoại")
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid CustomerLoginRequest request, HttpServletRequest http) {
        rateLimiter.check("login", CustomerAuthRateLimiter.clientKey(http), 20, Duration.ofMinutes(10));
        return withSession(authService.login(request));
    }

    @Operation(summary = "Làm mới access token của khách")
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.CUSTOMER_NAME, required = false) String refreshToken) {
        return withSession(authService.refresh(refreshToken));
    }

    @Operation(summary = "Đăng xuất khách")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.CUSTOMER_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clearCustomer())
                .build();
    }

    @Operation(summary = "Gửi link đặt lại mật khẩu", description = "Luôn trả 204 dù email có tồn tại hay không")
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimiter.check("forgot", CustomerAuthRateLimiter.clientKey(http), 5, Duration.ofHours(1));
        rateLimiter.check("forgot-email", request.email().trim().toLowerCase(Locale.ROOT), 3, Duration.ofHours(1));
        passwordResetService.requestReset(request.email());
    }

    @Operation(summary = "Đặt lại mật khẩu bằng link trong thư")
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid CustomerResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
    }

    private ResponseEntity<AuthenticationResponse> withSession(AuthDTO auth) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.createCustomer(auth.refreshToken()))
                .body(new AuthenticationResponse(auth.accessToken()));
    }
}
