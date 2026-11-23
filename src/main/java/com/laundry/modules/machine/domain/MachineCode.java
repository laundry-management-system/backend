package com.laundry.modules.machine.domain;

import com.laundry.common.exception.BusinessException;

import java.text.Normalizer;

/** Mã máy dạng {@code W-LG-001}: loại (W giặt / D sấy) - 3 ký tự đầu của hãng - số thứ tự theo loại + hãng. */
public final class MachineCode {

    private MachineCode() {}

    public static String brandShort(String brand) {
        String plain = Normalizer.normalize(brand == null ? "" : brand, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();
        if (plain.isEmpty()) {
            throw new BusinessException("Hãng máy phải có chữ hoặc số", "brand");
        }
        return plain.substring(0, Math.min(3, plain.length()));
    }

    public static String prefix(MachineType type, String brand) {
        return (type == MachineType.WASHER ? "W" : "D") + "-" + brandShort(brand) + "-";
    }

    public static String format(String prefix, int sequence) {
        return prefix + String.format("%03d", sequence);
    }

    /** Số thứ tự của mã có cùng tiền tố; mã khác mẫu (VD mã cũ W01) trả 0. */
    public static int sequenceOf(String code, String prefix) {
        if (code == null || !code.toUpperCase().startsWith(prefix)) return 0;
        String rest = code.substring(prefix.length());
        return rest.matches("\\d+") ? Integer.parseInt(rest) : 0;
    }
}
