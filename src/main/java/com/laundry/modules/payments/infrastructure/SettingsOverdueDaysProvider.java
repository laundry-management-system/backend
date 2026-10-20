package com.laundry.modules.payments.infrastructure;

import com.laundry.modules.payments.application.port.OverdueDaysProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsOverdueDaysProvider implements OverdueDaysProvider {

    private final EntityManager entityManager;

    @Value("${app.orders.overdue-days:7}")
    private int defaultDays;

    @Override
    public int getDays() {
        try {
            Object value = entityManager.createNativeQuery(
                            "SELECT setting_value FROM settings WHERE setting_key = :settingKey")
                    .setParameter("settingKey", "OVERDUE_DAYS")
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElse(null);
            int configured = Integer.parseInt(value == null ? "" : value.toString().trim());
            return configured > 0 ? configured : defaultDays;
        } catch (PersistenceException | DataAccessException | NumberFormatException ex) {
            return defaultDays;
        }
    }
}
