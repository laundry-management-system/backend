package com.laundry.modules.auth.application.dto;

public record PasswordSetupRequestedEvent(String email, String username, String link) {}
