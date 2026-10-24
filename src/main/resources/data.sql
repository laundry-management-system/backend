-- Schema and indexes are installed separately by the database migration.
-- Keep this initializer harmless when the application connects with a
-- restricted runtime role that must not own or alter shared tables.

SELECT 1;

CREATE SEQUENCE IF NOT EXISTS employee_code_seq START WITH 2;

INSERT INTO users (id, employee_code, full_name, phone, email, username, password_hash, role, status, hired_date, created_at, updated_at)
VALUES (1, 'NV01', 'Admin', '0900000000', 'tunhcaibe10@gmail.com', 'admin',
        '$2a$10$khyNcyj/YzTH/aSq7TC3w..90d3JmpNfjuCSfvbSiSqV8zqPjW8A6',
        'ADMIN', 'ACTIVE', CURRENT_DATE, NOW(), NOW())
    ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, employee_code, full_name, phone, email, username, password_hash, role, status, hired_date, created_at, updated_at)
VALUES (0, 'NV00', 'system', '0000000000', 'system@gmail.com', 'system',
        '$2a$10$khyNcyj/YzTH/aSq7TC3w..90d3JmpNfjuCSfvbSiSqV8zqPjW8A6',
        'ADMIN', 'INACTIVE', CURRENT_DATE, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

INSERT INTO settings (setting_key, setting_value, description, updated_by, updated_at)
VALUES
    ('MAX_LOGIN_FAIL', '5', 'Số lần đăng nhập sai tối đa trước khi bị khóa', NULL, NOW()),
    ('LOCK_MINUTES', '15', 'Thời gian khóa tài khoản sau khi đăng nhập sai quá số lần quy định (tính theo phút)', NULL, NOW()),

    ('OPENING_TIME', '08:00', 'Giờ mở cửa của cửa hàng', NULL, NOW()),
    ('CLOSING_TIME', '21:00', 'Giờ đóng cửa của cửa hàng', NULL, NOW()),
    ('OVERDUE_DAYS', '7', 'Số ngày quá hạn trước khi đơn hàng được xem là quá hạn', NULL, NOW()),

    ('BANK_BIN', '970422', 'Mã BIN ngân hàng nhận thanh toán', NULL, NOW()),
    ('BANK_ACCOUNT_NO', '0000000000', 'Số tài khoản ngân hàng nhận thanh toán', NULL, NOW()),
    ('BANK_ACCOUNT_NAME', 'CHU TIEM GIAT UI', 'Tên chủ tài khoản ngân hàng nhận thanh toán', NULL, NOW()),

    ('COMPENSATION_TERMS', '100000', 'Mức bồi thường mặc định khi xảy ra sự cố làm mất hoặc hư hỏng đồ', NULL, NOW()),
    ('STORAGE_DAYS', '30', 'Số ngày lưu trữ đồ sau khi hoàn thành đơn hàng', NULL, NOW()),

    ('STORE_NAME', 'Laundry Shop', 'Tên cửa hàng giặt ủi', NULL, NOW()),
    ('STORE_ADDRESS', 'Chưa cập nhật', 'Địa chỉ cửa hàng giặt ủi', NULL, NOW())
ON CONFLICT (setting_key) DO NOTHING;