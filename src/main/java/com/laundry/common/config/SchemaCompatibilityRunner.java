package com.laundry.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Giữ DB cũ tương thích khi enum được thêm giá trị mới.
 *
 * <p>Hibernate sinh ràng buộc {@code CHECK (col IN (...))} cho cột {@code @Enumerated(STRING)} lúc tạo bảng,
 * nhưng {@code ddl-auto: update} không cập nhật lại ràng buộc đó. DB tạo trước khi thêm giá trị enum sẽ từ chối
 * giá trị mới (ví dụ {@code ORDER_CANCEL}). Runner này gỡ các ràng buộc đó; kiểu enum đã được Java kiểm soát.
 *
 * <p>Mọi lệnh đều bọc try/catch: tài khoản DB không có quyền ALTER thì chỉ ghi cảnh báo, không chặn khởi động.
 * Môi trường dùng migration riêng cần chạy các lệnh tương đương trong migration.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaCompatibilityRunner implements ApplicationRunner {

    private static final List<String> ENUM_CHECKS = List.of(
            "audit_logs:audit_logs_action_check",
            "audit_logs:audit_logs_entity_type_check",
            "order_status_history:order_status_history_action_type_check",
            "settings:settings_setting_key_check"
    );

    /** Tham số thêm sau khi data.sql đã chạy trên DB cũ; data.sql không chèn được vì ràng buộc CHECK ở trên. */
    private static final List<String[]> NEW_SETTINGS = List.of(
            new String[]{"STORE_PHONE", "0900000000", "Số điện thoại / hotline in trên phiếu và hóa đơn"},
            new String[]{"ORDER_CODE_PREFIX", "LD", "Tiền tố mã đơn hàng (1–2 ký tự in hoa hoặc số)"}
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String entry : ENUM_CHECKS) {
            String[] parts = entry.split(":");
            execute("ALTER TABLE IF EXISTS " + parts[0] + " DROP CONSTRAINT IF EXISTS " + parts[1]);
        }
        for (String[] s : NEW_SETTINGS) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO settings (setting_key, setting_value, description, updated_by, updated_at)
                        VALUES (?, ?, ?, NULL, NOW())
                        ON CONFLICT (setting_key) DO NOTHING
                        """, s[0], s[1], s[2]);
            } catch (Exception e) {
                log.warn("Không nạp được tham số {}: {}", s[0], e.getMessage());
            }
        }
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Bỏ qua lệnh tương thích schema [{}]: {}", sql, e.getMessage());
        }
    }
}
