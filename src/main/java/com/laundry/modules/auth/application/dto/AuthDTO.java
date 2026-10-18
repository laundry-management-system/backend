package com.laundry.modules.auth.application.dto;

public record AuthDTO(
        String accessToken,
        String refreshToken
) {}
