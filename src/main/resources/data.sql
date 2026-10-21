-- Dữ liệu mẫu (tài khoản admin, danh mục...) chạy sau khi Hibernate tạo bảng.
-- Chưa có bảng nghiệp vụ nên để trống; thêm INSERT ... ON CONFLICT DO NOTHING khi có module.
SELECT 1;

CREATE SEQUENCE IF NOT EXISTS employee_code_seq START WITH 2;

INSERT INTO users (employee_code, full_name, phone, email, username, password_hash, role, status, hired_date, created_at, updated_at)
VALUES ('NV01', 'Admin', '0900000000', 'tunhcaibe10@gmail.com', 'admin',
        '$2a$10$khyNcyj/YzTH/aSq7TC3w..90d3JmpNfjuCSfvbSiSqV8zqPjW8A6',
        'ADMIN', 'ACTIVE', CURRENT_DATE, NOW(), NOW())
    ON CONFLICT (username) DO NOTHING;
