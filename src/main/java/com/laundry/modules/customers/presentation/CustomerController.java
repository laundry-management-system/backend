package com.laundry.modules.customers.presentation;

import com.laundry.modules.customers.application.CustomerService;
import com.laundry.modules.customers.application.dto.CreateCustomerRequest;
import com.laundry.modules.customers.application.dto.CustomerResponse;
import com.laundry.modules.customers.application.dto.CustomerStatusRequest;
import com.laundry.modules.customers.application.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'EMPLOYEE')")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request, Authentication authentication) {
        return customerService.create(request, authentication.getName());
    }

    @GetMapping
    public List<CustomerResponse> search(@RequestParam(required = false) String phone,
                                         @RequestParam(required = false) String keyword) {
        if (phone != null && !phone.isBlank()) {
            return List.of(customerService.getByPhone(phone));
        }
        return customerService.search(keyword);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable long id) {
        return customerService.getById(id);
    }

    @PutMapping("/{id}")
    public CustomerResponse updateProfile(@PathVariable long id,
                                          @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.updateProfile(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerResponse changeStatus(@PathVariable long id,
                                         @Valid @RequestBody CustomerStatusRequest request) {
        return customerService.changeStatus(id, request);
    }
}
