package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank(message = "Customer full name must not be blank (BR-B02)")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Phone number must not be blank (BR-B02)")
        @Pattern(regexp = "^0\\d{9}$", message = "Phone number must contain 10 digits starting with 0 (BR-B01)")
        String phone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}
