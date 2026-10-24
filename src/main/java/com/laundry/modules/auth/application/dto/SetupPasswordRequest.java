package com.laundry.modules.auth.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request chứa mật khẩu mới khi kích hoạt tài khoản lần đầu tiên")
public record SetupPasswordRequest(String password) {}
