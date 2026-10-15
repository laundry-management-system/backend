package com.laundry.modules.customer.presentation;

import com.laundry.modules.customer.application.CustomerService;
import com.laundry.modules.customer.application.dto.ChangeCustomerStatusRequest;
import com.laundry.modules.customer.application.dto.CreateCustomerRequest;
import com.laundry.modules.customer.application.dto.CustomerResponse;
import com.laundry.modules.customer.application.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Customer Management", description = "Quản lý hồ sơ khách hàng (M3 - Thành viên 2: UC02, BM1, QĐ1, BR-B)")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Tiếp nhận & tạo hồ sơ khách hàng mới (UC02 / BM1)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return customerService.createCustomer(request, staff);
    }

    @Operation(summary = "Tra cứu khách hàng theo Số điện thoại (UC02 / BM1 - Khóa tra cứu chính BR-B01)")
    @GetMapping("/by-phone/{phone}")
    public CustomerResponse getCustomerByPhone(@PathVariable String phone) {
        return customerService.getCustomerByPhone(phone);
    }

    @Operation(summary = "Tra cứu khách hàng theo Mã khách hàng (KHxxxxxx)")
    @GetMapping("/by-code/{code}")
    public CustomerResponse getCustomerByCode(@PathVariable String code) {
        return customerService.getCustomerByCode(code);
    }

    @Operation(summary = "Xem chi tiết hồ sơ khách hàng theo ID (BM1)")
    @GetMapping("/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @Operation(summary = "Danh sách khách hàng (tìm kiếm theo tên, SĐT, mã; hỗ trợ phân trang)")
    @GetMapping
    public Page<CustomerResponse> listCustomers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return customerService.listCustomers(search, pageable);
    }

    @Operation(summary = "Cập nhật thông tin khách hàng (UC02)")
    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return customerService.updateCustomer(id, request, staff);
    }

    @Operation(summary = "Thay đổi trạng thái hoạt động (ACTIVE / INACTIVE) - Không xóa dữ liệu theo BR-B03")
    @PatchMapping("/{id}/status")
    public CustomerResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeCustomerStatusRequest request
    ) {
        return customerService.changeStatus(id, request.status());
    }

    private String getUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return "STAFF";
        }
        return authentication.getName();
    }
}
