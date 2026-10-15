package com.laundry.modules.auth.presentation;

import com.laundry.common.security.RefreshTokenCookie;
import com.laundry.modules.auth.application.AuthService;
import com.laundry.modules.auth.application.dto.AuthDTO;
import com.laundry.modules.auth.application.dto.AuthenticationResponse;
import com.laundry.modules.auth.application.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication Management", description = "API xác thực người dùng")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("api/auth")
public class AuthController {

    AuthService authService;
    RefreshTokenCookie refreshTokenCookie;

    @Operation(summary = "Đăng nhập", description = "API đăng nhập và nhận token JWT")
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        AuthDTO authDTO = authService.login(loginRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(authDTO.refreshToken()))
                .body(new AuthenticationResponse(authDTO.accessToken()));
    }
}
