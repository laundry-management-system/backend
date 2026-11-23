package com.laundry.modules.customer.presentation;

import com.laundry.common.security.RefreshTokenCookie;
import com.laundry.modules.auth.application.dto.AuthDTO;
import com.laundry.modules.auth.application.dto.AuthenticationResponse;
import com.laundry.modules.customer.application.CustomerPortalService;
import com.laundry.modules.customer.application.dto.ChangeMyPasswordRequest;
import com.laundry.modules.customer.application.dto.CustomerProfileResponse;
import com.laundry.modules.customer.application.dto.UpdateMyProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Portal", description = "Khách xem và sửa thông tin của mình")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer-portal/me")
public class CustomerPortalController {

    private final CustomerPortalService portalService;
    private final RefreshTokenCookie refreshTokenCookie;

    @Operation(summary = "Thông tin của khách đang đăng nhập")
    @GetMapping
    public CustomerProfileResponse getMe(Authentication authentication) {
        return portalService.getMe(authentication);
    }

    @Operation(summary = "Khách sửa họ tên, email, địa chỉ")
    @PutMapping
    public CustomerProfileResponse updateMe(Authentication authentication, @RequestBody @Valid UpdateMyProfileRequest request) {
        return portalService.updateMe(authentication, request);
    }

    @Operation(summary = "Khách đổi mật khẩu", description = "Trả token mới; các phiên khác bị đăng xuất")
    @PutMapping("/password")
    public ResponseEntity<AuthenticationResponse> changePassword(Authentication authentication,
                                                                 @RequestBody @Valid ChangeMyPasswordRequest request) {
        AuthDTO auth = portalService.changePassword(authentication, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.createCustomer(auth.refreshToken()))
                .body(new AuthenticationResponse(auth.accessToken()));
    }
}
