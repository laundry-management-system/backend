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

@Tag(name = "Customer Management", description = "Customer profile management (M3 - Member 2: UC02, BM1, QD1, BR-B)")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Create customer profile (UC02 / BM1)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            Authentication authentication
    ) {
        String staff = getUsername(authentication);
        return customerService.createCustomer(request, staff);
    }

    @Operation(summary = "Find customer by phone number (UC02 / BM1 - Primary lookup key BR-B01)")
    @GetMapping("/by-phone/{phone}")
    public CustomerResponse getCustomerByPhone(@PathVariable String phone) {
        return customerService.getCustomerByPhone(phone);
    }

    @Operation(summary = "Find customer by customer code (KHxxxxxx)")
    @GetMapping("/by-code/{code}")
    public CustomerResponse getCustomerByCode(@PathVariable String code) {
        return customerService.getCustomerByCode(code);
    }

    @Operation(summary = "Get customer details by ID (BM1)")
    @GetMapping("/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @Operation(summary = "List customers (search by name, phone, code; supports pagination)")
    @GetMapping
    public Page<CustomerResponse> listCustomers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return customerService.listCustomers(search, pageable);
    }

    @Operation(summary = "Update customer details (UC02)")
    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return customerService.updateCustomer(id, request);
    }

    @Operation(summary = "Change customer status (ACTIVE / INACTIVE) - Soft deactivate per BR-B03")
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