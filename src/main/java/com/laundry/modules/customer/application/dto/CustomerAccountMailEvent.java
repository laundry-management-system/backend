package com.laundry.modules.customer.application.dto;

public record CustomerAccountMailEvent(String to, String subject, String html) {}
