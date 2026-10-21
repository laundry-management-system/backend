package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response chứa access token")
public record AuthenticationResponse(
        @Schema(description = "Access Token dùng để xác thực và phân quyền")
        String accessToken
) {
}
