package com.laundry.modules.shift.domain;

import com.laundry.common.exception.BusinessException;
import com.laundry.common.exception.ResourceConflictException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Phép tính tiền của ca (BR-K02, BR-K04 – BR-K07). Hàm thuần, không phụ thuộc Spring. */
public final class ShiftMath {

    public record ShiftTotals(BigDecimal cashIn, int cashInCount, BigDecimal cashOut, int cashOutCount,
                              BigDecimal transferTotal, int transferCount) {

        public static final ShiftTotals EMPTY =
                new ShiftTotals(BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
    }

    public record ClosingSnapshot(ShiftTotals totals, BigDecimal expected, BigDecimal counted,
                                  BigDecimal difference, String differenceReason, String denominationsJson,
                                  BigDecimal keep, BigDecimal handIn) {
    }

    /** Mệnh giá tiền mặt VND được đếm khi kết ca. */
    public static final Set<Integer> DENOMINATIONS =
            Set.of(500_000, 200_000, 100_000, 50_000, 20_000, 10_000, 5_000, 2_000, 1_000);

    /** Đối chiếu chuyển khoản: số trong app ngân hàng, chênh lệch so với hệ thống và lý do. */
    public record TransferCheck(BigDecimal bankTotal, BigDecimal difference, String reason) {
    }

    private ShiftMath() {
    }

    /**
     * Chuyển khoản lệch xử lý như chênh lệch tiền mặt: vẫn kết ca được nhưng phải ghi lý do.
     * {@code bankTotal} null (client cũ không gửi) → coi như bằng tổng hệ thống.
     */
    public static TransferCheck transferCheck(BigDecimal systemTotal, BigDecimal bankTotal, String reason) {
        if (bankTotal != null && bankTotal.signum() < 0) {
            throw new BusinessException("Tổng tiền trong ứng dụng ngân hàng phải từ 0 trở lên", "bankTotal");
        }
        BigDecimal actual = bankTotal == null ? systemTotal : bankTotal;
        BigDecimal difference = actual.subtract(systemTotal);
        String trimmed = isBlank(reason) ? null : reason.trim();
        if (difference.signum() != 0 && trimmed == null) {
            throw new BusinessException("Chuyển khoản lệch với ứng dụng ngân hàng, vui lòng ghi lý do",
                    "transferDifferenceReason");
        }
        return new TransferCheck(actual, difference, difference.signum() == 0 ? null : trimmed);
    }

    /**
     * Bảng đếm theo mệnh giá → JSON lưu cùng ca (mệnh giá lớn trước, bỏ dòng 0 tờ); null khi bảng rỗng.
     */
    public static String denominationsJson(Map<Integer, Integer> counts) {
        if (counts == null) {
            return null;
        }
        Map<Integer, Integer> sorted = new TreeMap<>(Comparator.reverseOrder());
        counts.forEach((value, count) -> {
            if (!DENOMINATIONS.contains(value)) {
                throw new BusinessException("Mệnh giá không hợp lệ: " + value, "denominations");
            }
            if (count == null || count < 0) {
                throw new BusinessException("Số tờ phải từ 0 trở lên", "denominations");
            }
            if (count > 0) {
                sorted.put(value, count);
            }
        });
        return sorted.isEmpty() ? null : sorted.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + e.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }

    /**
     * Bắt buộc đếm theo mệnh giá khi kết ca: tiền mặt thực đếm phải đúng bằng tổng các dòng mệnh giá
     * (FE chỉ hiển thị tổng, không cho nhập tay).
     */
    public static void validateCountedByDenominations(Map<Integer, Integer> counts, BigDecimal counted) {
        long total = counts == null ? 0 : counts.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 0)
                .mapToLong(e -> (long) e.getKey() * e.getValue())
                .sum();
        if (denominationsJson(counts) == null) {
            throw new BusinessException("Vui lòng đếm tiền mặt theo mệnh giá", "denominations");
        }
        if (counted == null || counted.compareTo(BigDecimal.valueOf(total)) != 0) {
            throw new BusinessException("Tiền mặt thực đếm phải bằng tổng đếm theo mệnh giá", "countedCash");
        }
    }

    /**
     * Tiền để lại két cũng tính theo tờ: mỗi mệnh giá không vượt số tờ đã đếm, tổng phải bằng keepCash.
     * Trả JSON để lưu cùng ca (null khi không để lại tờ nào).
     */
    public static String validateKeepByDenominations(Map<Integer, Integer> keepCounts,
                                                     Map<Integer, Integer> countedCounts, BigDecimal keepCash) {
        String json = denominationsJson(keepCounts);
        long total = 0;
        if (keepCounts != null) {
            for (Map.Entry<Integer, Integer> e : keepCounts.entrySet()) {
                int keep = e.getValue() == null ? 0 : e.getValue();
                int counted = countedCounts == null ? 0 : countedCounts.getOrDefault(e.getKey(), 0);
                if (keep > counted) {
                    throw new BusinessException("Để lại két " + keep + " tờ " + e.getKey()
                            + "đ nhưng chỉ đếm được " + counted + " tờ", "keepDenominations");
                }
                total += (long) e.getKey() * keep;
            }
        }
        if (keepCash == null || keepCash.compareTo(BigDecimal.valueOf(total)) != 0) {
            throw new BusinessException("Tiền để lại két phải bằng tổng số tờ để lại", "keepCash");
        }
        return json;
    }

    public static BigDecimal expected(BigDecimal opening, ShiftTotals totals) {
        return opening.add(totals.cashIn()).subtract(totals.cashOut());
    }

    /** Tiền đầu ca ≥ 0; có số gợi ý mà nhập khác thì bắt buộc lý do. */
    public static void validateOpening(BigDecimal opening, BigDecimal suggested, String reason) {
        if (opening == null || opening.signum() < 0) {
            throw new BusinessException("Tiền đầu ca phải từ 0 trở lên", "openingCash");
        }
        if (suggested != null && opening.compareTo(suggested) != 0 && isBlank(reason)) {
            throw new BusinessException("Tiền đầu ca khác số gợi ý, vui lòng ghi lý do", "openingReason");
        }
    }

    /**
     * Kiểm ràng buộc kết ca theo thứ tự trong data-model.md rồi trả số liệu để chụp vào ca.
     * {@code expectedSeen} là số dự kiến người dùng đang thấy; khác số thực tế nghĩa là có giao dịch mới.
     */
    public static ClosingSnapshot validateClose(BigDecimal opening, ShiftTotals totals, BigDecimal expectedSeen,
                                                BigDecimal counted, BigDecimal keep, boolean bankReconciled,
                                                String differenceReason, String denominationsJson) {
        if (counted == null || counted.signum() < 0) {
            throw new BusinessException("Tiền mặt thực đếm phải từ 0 trở lên", "countedCash");
        }
        if (keep == null || keep.signum() < 0) {
            throw new BusinessException("Tiền để lại két phải từ 0 trở lên", "keepCash");
        }
        if (keep.compareTo(counted) > 0) {
            throw new BusinessException("Tiền để lại két không được vượt quá tiền mặt thực đếm", "keepCash");
        }
        if (!bankReconciled) {
            throw new BusinessException("Cần xác nhận đã đối chiếu chuyển khoản với ứng dụng ngân hàng",
                    "bankReconciled");
        }
        BigDecimal expected = expected(opening, totals);
        BigDecimal difference = counted.subtract(expected);
        String reason = isBlank(differenceReason) ? null : differenceReason.trim();
        if (difference.signum() != 0 && reason == null) {
            throw new BusinessException("Chênh lệch khác 0, vui lòng ghi lý do", "differenceReason");
        }
        if (expectedSeen == null || expectedSeen.compareTo(expected) != 0) {
            throw new ResourceConflictException("Số tiền dự kiến đã thay đổi do có giao dịch mới, vui lòng kiểm lại");
        }
        return new ClosingSnapshot(totals, expected, counted, difference, reason, denominationsJson,
                keep, counted.subtract(keep));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
