-- Tài khoản khách test cho trang công khai (đăng nhập, /dat-don). CHỈ dùng cho DB dev.
--
-- Chạy:   docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/seed-dev-customer.sql
-- Chạy lại bao nhiêu lần cũng được.
--
-- Đăng nhập bằng SĐT 0999990011 hoặc email seed-customer@example.test, mật khẩu: Seed013-fcf0d0f7

INSERT INTO customers (customer_code, full_name, phone, email, address, is_vip, status, source, password_hash, password_changed_at, created_by, created_at, updated_at)
VALUES ('KH9950', 'Seed Khách hàng', '0999990011', 'seed-customer@example.test', 'Phường Bến Nghé, Quận 1', false, 'ACTIVE', 'SELF_REGISTERED',
        '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa', NOW(), 'seed-customer', NOW(), NOW())
ON CONFLICT (phone) DO UPDATE SET password_hash = EXCLUDED.password_hash, status = 'ACTIVE', email = EXCLUDED.email, updated_at = NOW();
