package com.laundry.modules.payments.infrastructure;

import com.laundry.modules.payments.application.port.OverdueDaysProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsOverdueDaysProvider implements OverdueDaysProvider {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.orders.overdue-days:7}")
    private int defaultDays;

    @Override
    public int getDays() {
        try {
            String value = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM settings WHERE setting_key = 'OVERDUE_DAYS'", String.class);
            int configured = Integer.parseInt(value);
            return configured > 0 ? configured : defaultDays;
        } catch (DataAccessException | NumberFormatException ex) {
            return defaultDays;
        }
    }
}
