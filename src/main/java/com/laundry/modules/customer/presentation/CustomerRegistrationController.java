package com.laundry.modules.customer.presentation;

import com.laundry.modules.customer.application.CustomerRegistrationService;
import com.laundry.modules.customer.application.dto.ActivateRegistrationResponse;
import com.laundry.modules.customer.application.dto.PendingCountResponse;
import com.laundry.modules.customer.application.dto.RegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Registrations", description = "Admin kích hoạt hoặc từ chối tài khoản khách tự đăng ký")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer-registrations")
@PreAuthorize("hasRole('ADMIN')")
public class CustomerRegistrationController {

    private final CustomerRegistrationService registrationService;

    @Operation(summary = "Danh sách tài khoản chờ kích hoạt, cũ nhất trước")
    @GetMapping
    public Page<RegistrationResponse> listPending(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return registrationService.listPending(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100)));
    }

    @Operation(summary = "Số tài khoản đang chờ kích hoạt")
    @GetMapping("/pending-count")
    public PendingCountResponse pendingCount() {
        return new PendingCountResponse(registrationService.pendingCount());
    }

    @Operation(summary = "Kích hoạt: gắn vào hồ sơ trùng SĐT hoặc tạo hồ sơ mới")
    @PostMapping("/{id}/activate")
    public ActivateRegistrationResponse activate(@PathVariable Long id, Authentication authentication) {
        return registrationService.activate(id, authentication.getName());
    }

    @Operation(summary = "Từ chối yêu cầu đăng ký")
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long id, Authentication authentication) {
        registrationService.reject(id, authentication.getName());
    }
}
