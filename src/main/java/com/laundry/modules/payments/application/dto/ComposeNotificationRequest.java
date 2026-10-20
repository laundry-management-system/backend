package com.laundry.modules.payments.application.dto;

import jakarta.validation.constraints.NotNull;

public record ComposeNotificationRequest(
        @NotNull(message = "Kênh nhắn khách không được để trống")
        NotificationChannel channel
) {
    public enum NotificationChannel {
        ZALO,
        SMS
    }
}
