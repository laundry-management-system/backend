package com.laundry.modules.customer.application.dto;

public record ActivateRegistrationResponse(Long customerId, String customerCode, boolean linked) {}
