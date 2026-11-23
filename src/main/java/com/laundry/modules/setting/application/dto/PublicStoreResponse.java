package com.laundry.modules.setting.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin cửa hàng công khai cho trang chủ (không đăng nhập)")
public record PublicStoreResponse(
        String name,
        @Schema(nullable = true) String address,
        @Schema(nullable = true, description = "10 chữ số, bắt đầu bằng 0") String phone,
        @Schema(example = "08:00") String openingTime,
        @Schema(example = "21:00") String closingTime,
        int whitesFee,
        int compensationMultiplier,
        int compensationCapPerItem,
        int storageDays
) {
    // Giá trị seed của STORE_ADDRESS, coi như chưa có địa chỉ (R13)
    private static final String ADDRESS_PLACEHOLDER = "Chưa cập nhật";

    public PublicStoreResponse {
        address = blankToNull(address);
        if (ADDRESS_PLACEHOLDER.equals(address)) {
            address = null;
        }
        phone = blankToNull(phone);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
