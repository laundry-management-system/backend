package com.laundry.modules.customer.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank(message = "Họ và tên khách hàng không được để trống (BR-B02)")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số bắt đầu bằng 0 (BR-B01)")
        String phone,

        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        String address,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {}
