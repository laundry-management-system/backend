-- Schema and indexes are installed separately by the database migration.
-- Keep this initializer harmless when the application connects with a
-- restricted runtime role that must not own or alter shared tables.
SELECT 1;

CREATE SEQUENCE IF NOT EXISTS employee_code_seq START WITH 2;

INSERT INTO users (employee_code, full_name, phone, email, username, password_hash, role, status, hired_date, created_at, updated_at)
VALUES ('NV01', 'Admin', '0900000000', 'tunhcaibe10@gmail.com', 'admin',
        '$2a$10$khyNcyj/YzTH/aSq7TC3w..90d3JmpNfjuCSfvbSiSqV8zqPjW8A6',
        'ADMIN', 'ACTIVE', CURRENT_DATE, NOW(), NOW())
    ON CONFLICT (username) DO NOTHING;

-- Bảng giá mẫu (BM2) cho tiệm giặt sấy 1 chi nhánh: chỉ nạp khi chưa có dịch vụ nào.
-- Giặt nhanh luôn ngắn hơn thời gian chuẩn (BR-C06); đồ trắng chỉ áp cho quần áo giặt máy.
INSERT INTO services (service_code, name, unit, current_price, min_quantity, standard_hours,
                      express_supported, express_surcharge, express_hours, whites_applicable, status, created_at, updated_at)
SELECT v.* FROM (VALUES
    -- Quần áo thường: tính theo kg, giặt + sấy + gấp
    ('DV01', 'Giặt sấy quần áo', 'KG', 30000, 3, 24, true, 10000, 6, true, 'ACTIVE', NOW(), NOW()),
    ('DV02', 'Giặt sấy + ủi quần áo', 'KG', 45000, 3, 48, true, 15000, 24, true, 'ACTIVE', NOW(), NOW()),
    ('DV03', 'Ủi quần áo', 'PIECE', 10000, 5, 24, true, 5000, 6, false, 'ACTIVE', NOW(), NOW()),
    ('DV04', 'Giặt hấp vest / áo dài', 'SET', 120000, 1, 72, true, 50000, 24, false, 'ACTIVE', NOW(), NOW()),
    -- Đồ lớn: tính theo kg, máy công suất lớn, sấy lâu
    ('DV05', 'Chăn / mền / mùng / drap', 'KG', 80000, 3, 48, true, 20000, 24, false, 'ACTIVE', NOW(), NOW()),
    ('DV06', 'Rèm cửa', 'KG', 60000, 3, 72, false, NULL::numeric, NULL::int, false, 'ACTIVE', NOW(), NOW()),
    ('DV07', 'Topper (tấm phủ nệm)', 'KG', 100000, 3, 72, false, NULL::numeric, NULL::int, false, 'ACTIVE', NOW(), NOW()),
    -- Đồ đặc biệt: tính theo sản phẩm, làm thủ công
    ('DV08', 'Giày', 'PRODUCT', 60000, 1, 48, true, 30000, 24, false, 'ACTIVE', NOW(), NOW()),
    ('DV09', 'Gấu bông', 'PRODUCT', 70000, 1, 72, false, NULL::numeric, NULL::int, false, 'ACTIVE', NOW(), NOW()),
    ('DV10', 'Balo / túi xách', 'PRODUCT', 80000, 1, 72, false, NULL::numeric, NULL::int, false, 'ACTIVE', NOW(), NOW())
) AS v
WHERE NOT EXISTS (SELECT 1 FROM services);

INSERT INTO users (id, employee_code, full_name, phone, email, username, password_hash, role, status, hired_date, created_at, updated_at)
VALUES (0, 'NV00', 'system', '0000000000', 'system@gmail.com', 'system',
        '$2a$10$khyNcyj/YzTH/aSq7TC3w..90d3JmpNfjuCSfvbSiSqV8zqPjW8A6',
        'ADMIN', 'INACTIVE', CURRENT_DATE, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

-- Điều khoản bồi thường dạng chữ đã thay bằng hệ số K + mức trần; dòng cũ làm enum SettingKey không đọc được
DELETE FROM settings WHERE setting_key = 'COMPENSATION_TERMS';

-- ddl-auto không cập nhật CHECK của enum cũ; bỏ nếu có để thêm STORE_PHONE, SERVICE_AREA_MANAGE, SERVICE_AREA
ALTER TABLE settings   DROP CONSTRAINT IF EXISTS settings_setting_key_check;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_entity_type_check;

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

    ('COMPENSATION_MULTIPLIER', '10', 'Hệ số bồi thường K: tối đa K lần giá dịch vụ của món', NULL, NOW()),
    ('COMPENSATION_CAP_PER_ITEM', '500000', 'Mức trần bồi thường mỗi món (đ)', NULL, NOW()),
    ('STORAGE_DAYS', '30', 'Số ngày lưu trữ đồ sau khi hoàn thành đơn hàng', NULL, NOW()),

    ('STORE_NAME', 'Laundry Shop', 'Tên cửa hàng giặt ủi', NULL, NOW()),
    ('STORE_ADDRESS', 'Chưa cập nhật', 'Địa chỉ cửa hàng giặt ủi', NULL, NOW()),
    ('STORE_PHONE', '', 'Số điện thoại cửa hàng (10 chữ số, bắt đầu bằng 0)', NULL, NOW()),

    ('WHITES_FEE', '80000', 'Phụ phí giặt riêng đồ trắng (đ/đơn)', NULL, NOW()),

    ('SHIFT_WARN_HOURS', '14', 'Cảnh báo ca mở quá số giờ này mà chưa kết', NULL, NOW()),

    ('BOOKING_ENABLED', 'true', 'Nhận đặt đơn online (true: bật, false: tạm ngừng)', NULL, NOW()),
    ('BOOKING_DELIVERY_ENABLED', 'true', 'Nhận lấy và giao tận nơi cho đơn đặt online (true: bật, false: tắt)', NULL, NOW()),
    ('BOOKING_MIN_LEAD_HOURS', '1', 'Số giờ đặt trước tối thiểu so với giờ hẹn của yêu cầu đặt đơn', NULL, NOW()),
    ('BOOKING_MAX_OPEN_PER_CUSTOMER', '10', 'Số yêu cầu đặt đơn chưa đóng tối đa của mỗi khách', NULL, NOW()),
    ('BOOKING_PICKUP_MIN_AMOUNT', '100000', 'Tiền hàng tạm tính tối thiểu để được lấy tận nơi (đ)', NULL, NOW()),
    ('BOOKING_RETURN_GAP_HOURS_NORMAL', '24', 'Thời gian giặt tiêu chuẩn', NULL, NOW()),
    ('BOOKING_RETURN_GAP_HOURS_EXPRESS', '6', 'Thời gian giặt nhanh', NULL, NOW())
ON CONFLICT (setting_key) DO NOTHING;

UPDATE settings SET setting_value='10' WHERE setting_key='BOOKING_MAX_OPEN_PER_CUSTOMER' AND setting_value='2' AND updated_by IS NULL;

-- Tối đa một ca đang mở (BR-K01); Hibernate ddl-auto không tạo được index một phần
CREATE UNIQUE INDEX IF NOT EXISTS uk_shifts_one_open ON shifts ((true)) WHERE status = 'OPEN';
CREATE INDEX IF NOT EXISTS idx_payments_shift ON payments (shift_id);
CREATE INDEX IF NOT EXISTS idx_payments_resolved_shift ON payments (resolved_shift_id);

-- Tên khu vực không trùng, không phân biệt hoa thường; chặn cả khi hai Admin thêm cùng lúc (FR-029e)
CREATE UNIQUE INDEX IF NOT EXISTS uk_service_areas_name_ci ON service_areas (lower(name));

-- Tài khoản khách: hồ sơ cũ là do nhân viên tạo; enum lưu dạng chuỗi nên bỏ CHECK để thêm giá trị sau không vỡ
UPDATE customers SET source = 'COUNTER' WHERE source IS NULL;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_source_check;
ALTER TABLE customer_registrations DROP CONSTRAINT IF EXISTS customer_registrations_status_check;
CREATE UNIQUE INDEX IF NOT EXISTS uk_customers_email_ci ON customers (lower(email)) WHERE email IS NOT NULL;
-- Chỉ yêu cầu đang chờ mới giữ SĐT/email; yêu cầu bị từ chối thì đăng ký lại được
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_registrations_pending_phone ON customer_registrations (phone) WHERE status = 'PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_registrations_pending_email ON customer_registrations (lower(email)) WHERE status = 'PENDING' AND email IS NOT NULL;
-- Yêu cầu chờ tạo trước khi hồ sơ được gắn lúc đăng ký: gắn vào hồ sơ cùng SĐT
UPDATE customer_registrations r SET customer_id = c.id FROM customers c
    WHERE r.status = 'PENDING' AND r.customer_id IS NULL AND c.phone = r.phone;

-- 029: đơn online (enum mở rộng, received_at NULL tới khi nhận đồ)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE order_status_history DROP CONSTRAINT IF EXISTS order_status_history_action_type_check;
ALTER TABLE orders ALTER COLUMN received_at DROP NOT NULL;
