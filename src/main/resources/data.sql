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

INSERT INTO settings (setting_key, setting_value) VALUES ('WHITES_FEE', '80000') ON CONFLICT DO NOTHING;
