package com.laundry.modules.setting.application.dto;

public record PublicBookingSettingsResponse(boolean bookingEnabled, boolean deliveryEnabled, int minLeadHours) {
}
