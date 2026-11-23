package com.laundry.modules.shift.domain;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Đọc dữ liệu của module khác (người dùng, cài đặt) bằng native query, không import module đó. */
@Repository
@RequiredArgsConstructor
public class ShiftLookupQuery {

    private static final int DEFAULT_WARN_HOURS = 14;

    private final EntityManager entityManager;

    public String userName(long userId) {
        List<?> rows = entityManager.createNativeQuery("SELECT full_name FROM users WHERE id = :id")
                .setParameter("id", userId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0).toString();
    }

    public int warnAfterHours() {
        List<?> rows = entityManager.createNativeQuery(
                        "SELECT setting_value FROM settings WHERE setting_key = 'SHIFT_WARN_HOURS'")
                .getResultList();
        try {
            return rows.isEmpty() ? DEFAULT_WARN_HOURS : Integer.parseInt(rows.get(0).toString().trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_WARN_HOURS;
        }
    }
}
