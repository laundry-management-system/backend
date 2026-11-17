package com.laundry.modules.incident.domain;

import com.laundry.common.exception.BusinessException;
import com.laundry.modules.order.domain.ItemUnit;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Quy tắc tiền của phiếu sự cố (BR-I04 – BR-I06). Hàm thuần, không phụ thuộc Spring. */
public final class IncidentMath {

    private IncidentMath() {
    }

    /** Mức tối đa = số món × min(K × giá dịch vụ của món, mức trần mỗi món); dịch vụ theo kg lấy đơn giá 1 kg. */
    public static BigDecimal maxCompensation(int affectedCount, BigDecimal unitPrice, int k, BigDecimal cap) {
        if (unitPrice == null || affectedCount < 1) {
            return BigDecimal.ZERO;
        }
        BigDecimal perItem = unitPrice.multiply(BigDecimal.valueOf(k)).min(cap);
        return perItem.multiply(BigDecimal.valueOf(affectedCount));
    }

    public static void validateAffectedCount(ItemUnit unit, BigDecimal itemQuantity, int n) {
        if (n < 1) {
            throw new BusinessException("Số món bị sự cố phải từ 1 trở lên", "affectedCount");
        }
        if (unit != ItemUnit.KG && itemQuantity != null && BigDecimal.valueOf(n).compareTo(itemQuantity) > 0) {
            throw new BusinessException("Số món bị sự cố từ 1 đến " + itemQuantity.stripTrailingZeros().toPlainString(),
                    "affectedCount");
        }
    }

    /** Kiểm số tiền theo phương án; Giặt lại luôn 0. Chỉ Admin lúc duyệt mới được vượt mức tối đa của Bồi thường. */
    public static BigDecimal normalizeAmount(IncidentResolution resolution, BigDecimal amount, BigDecimal lineTotal,
                                             BigDecimal max, boolean allowOverLimit) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.signum() < 0) {
            throw new BusinessException("Số tiền không được âm", "amount");
        }
        if (resolution == null) {
            return value;
        }
        return switch (resolution) {
            case REWASH -> BigDecimal.ZERO;
            case DISCOUNT -> {
                if (lineTotal == null || value.compareTo(lineTotal) > 0) {
                    throw new BusinessException("Giảm giá không vượt quá thành tiền của dòng ("
                            + money(lineTotal) + "đ)", "amount");
                }
                yield value;
            }
            case COMPENSATION -> {
                if (!allowOverLimit && value.compareTo(max) > 0) {
                    throw new BusinessException("Số tiền bồi thường vượt mức tối đa (" + money(max) + "đ)", "amount");
                }
                yield value;
            }
        };
    }

    public static String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));
        return format.format(value == null ? BigDecimal.ZERO : value);
    }
}
