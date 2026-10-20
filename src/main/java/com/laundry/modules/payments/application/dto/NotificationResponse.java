package com.laundry.modules.payments.application.dto;

import java.time.Instant;

public record NotificationResponse(
        String orderCode,
        ComposeNotificationRequest.NotificationChannel channel,
        String message,
        String deepLink,
        String publicLookupUrl,
        Instant lastNotifiedAt,
        int notifyCount
) {
}
