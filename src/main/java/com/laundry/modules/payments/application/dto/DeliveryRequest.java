package com.laundry.modules.payments.application.dto;

import jakarta.validation.constraints.Size;

public record DeliveryRequest(
        @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
        String receiverName,

        @Size(max = 50, message = "Quan hệ với khách tối đa 50 ký tự")
        String receiverRelation
) {
}
