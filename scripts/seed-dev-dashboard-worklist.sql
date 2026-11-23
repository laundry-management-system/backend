-- Dữ liệu test cho Tổng quan & Danh sách công việc (spec 013). CHỈ dùng cho DB dev.
--
-- Chạy:   docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/seed-dev-dashboard-worklist.sql
-- Chạy lại bao nhiêu lần cũng được: xóa dữ liệu seed cũ rồi tạo lại, mọi mốc thời gian tính theo lúc chạy.
--
-- Tài khoản test (mật khẩu chung): Seed013-fcf0d0f7
--   seed_admin  (Admin)      → Tổng quan
--   seed_staff  (Nhân viên)  → Danh sách công việc
--
-- Dữ liệu tạo ra (nhận diện bằng created_by = 'seed-013', invoice_no 'SEED013-…'):
--   Danh sách công việc (Mới nhận + Đang xử lý), 24 đơn:
--     2 chưa phân loại (1 hẹn mai, 1 Giặt nhanh còn 3 giờ)
--     Mới nhận đã phân loại: Giặt nhanh còn 5 giờ, thường hẹn 2 ngày, thường TRỄ 4 giờ
--     Đang xử lý: còn 6 giờ, TRỄ 1 ngày, Giặt nhanh TRỄ 30 phút, hẹn mai
--     + 15 đơn thường rải 1–5 ngày tới (để có trang 2)
--   Đồ tồn (Hoàn thành quá 7 ngày): 11, 9, 8 ngày, đã nhắc 2 / 1 / 0 lần; + 1 đơn Hoàn thành 2 ngày (chưa tồn)
--   Đã giao + thanh toán mỗi ngày trong 35 ngày qua (tiền mặt / chuyển khoản xen kẽ), 1 đơn hủy hôm nay

BEGIN;

-- Tài khoản test
INSERT INTO users (employee_code, full_name, phone, email, username, password_hash, role, status, hired_date, created_at, updated_at)
VALUES ('NV9901', 'Seed Admin', '0999990001', 'seed-admin@example.test', 'seed_admin', '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa', 'ADMIN', 'ACTIVE', CURRENT_DATE, NOW(), NOW()),
       ('NV9902', 'Seed Nhân viên', '0999990002', 'seed-staff@example.test', 'seed_staff', '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa', 'STAFF', 'ACTIVE', CURRENT_DATE, NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash, status = 'ACTIVE', updated_at = NOW();

-- Xóa seed cũ
DELETE FROM payments WHERE invoice_no LIKE 'SEED013-%';
DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE created_by = 'seed-013');
DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE created_by = 'seed-013');
DELETE FROM orders WHERE created_by = 'seed-013';
DELETE FROM customers WHERE created_by = 'seed-013';

-- 10 khách
INSERT INTO customers (customer_code, full_name, phone, is_vip, status, created_by, created_at, updated_at)
SELECT 'KH99' || lpad(n::text, 2, '0'),
       (ARRAY['Đỗ Quang Huy','Ngô Lan Anh','Bùi Văn Tài','Trần Thu Hà','Lê Minh Khoa',
              'Phạm Ngọc Mai','Võ Thành Đạt','Hoàng Yến Nhi','Đặng Gia Bảo','Lý Thảo Vy'])[n],
       '09990130' || lpad(n::text, 2, '0'), n = 1, 'ACTIVE', 'seed-013', NOW(), NOW()
FROM generate_series(1, 10) n;

CREATE TEMP TABLE seed_orders (
    n           int,
    status      text,
    order_type  text,
    received_at timestamptz,
    due_at      timestamptz,
    classified  boolean,
    completed_at timestamptz,
    delivered_at timestamptz,
    notify_count int,
    shelf       text,
    amount      numeric
) ON COMMIT DROP;

INSERT INTO seed_orders VALUES
-- Chưa phân loại
( 1, 'RECEIVED',   'NORMAL',  NOW() - interval '1 hour',   NOW() + interval '1 day',     false, NULL, NULL, 0, NULL, 0),
( 2, 'RECEIVED',   'EXPRESS', NOW() - interval '30 min',   NOW() + interval '3 hours',   false, NULL, NULL, 0, NULL, 0),
-- Mới nhận đã phân loại
( 3, 'RECEIVED',   'EXPRESS', NOW() - interval '2 hours',  NOW() + interval '5 hours',   true,  NULL, NULL, 0, NULL, 90000),
( 4, 'RECEIVED',   'NORMAL',  NOW() - interval '3 hours',  NOW() + interval '2 days',    true,  NULL, NULL, 0, NULL, 45000),
( 5, 'RECEIVED',   'NORMAL',  NOW() - interval '2 days',   NOW() - interval '4 hours',   true,  NULL, NULL, 0, NULL, 60000),
-- Đang xử lý
( 6, 'PROCESSING', 'NORMAL',  NOW() - interval '1 day',    NOW() + interval '6 hours',   true,  NULL, NULL, 0, NULL, 75000),
( 7, 'PROCESSING', 'NORMAL',  NOW() - interval '3 days',   NOW() - interval '1 day',     true,  NULL, NULL, 0, NULL, 50000),
( 8, 'PROCESSING', 'EXPRESS', NOW() - interval '5 hours',  NOW() - interval '30 min',    true,  NULL, NULL, 0, NULL, 120000),
( 9, 'PROCESSING', 'NORMAL',  NOW() - interval '6 hours',  NOW() + interval '1 day 2 hours', true, NULL, NULL, 0, NULL, 45000),
-- Đồ tồn và chờ lấy
(30, 'COMPLETED',  'NORMAL',  NOW() - interval '13 days',  NOW() - interval '12 days',   true,  NOW() - interval '11 days', NULL, 2, 'A3', 80000),
(31, 'COMPLETED',  'NORMAL',  NOW() - interval '11 days',  NOW() - interval '10 days',   true,  NOW() - interval '9 days',  NULL, 1, 'B1', 65000),
(32, 'COMPLETED',  'EXPRESS', NOW() - interval '9 days',   NOW() - interval '9 days' + interval '4 hours', true, NOW() - interval '8 days', NULL, 0, 'C2', 110000),
(33, 'COMPLETED',  'NORMAL',  NOW() - interval '3 days',   NOW() - interval '2 days',    true,  NOW() - interval '2 days',  NULL, 1, 'A1', 55000),
-- Hủy hôm nay
(34, 'CANCELLED',  'NORMAL',  NOW() - interval '2 hours',  NOW() + interval '1 day',     false, NULL, NULL, 0, NULL, 0);

-- 15 đơn thường để có trang 2
INSERT INTO seed_orders
SELECT 9 + k, CASE WHEN k % 2 = 0 THEN 'PROCESSING' ELSE 'RECEIVED' END, 'NORMAL',
       NOW() - (k || ' hours')::interval, NOW() + ((1 + k % 5) || ' days')::interval + (k || ' hours')::interval,
       true, NULL, NULL, 0, NULL, 30000 + k * 5000
FROM generate_series(1, 15) k;

-- Đã giao + đã thanh toán: 0–2 đơn mỗi ngày trong 35 ngày qua (hôm nay 2 đơn)
INSERT INTO seed_orders
SELECT 40 + d * 2 + j, 'DELIVERED', 'NORMAL',
       date_trunc('day', NOW() AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'Asia/Ho_Chi_Minh'
           - (d || ' days')::interval + interval '8 hours' + (j || ' hours')::interval,
       date_trunc('day', NOW() AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'Asia/Ho_Chi_Minh'
           - (d || ' days')::interval + interval '9 hours' + (j || ' hours')::interval,
       true,
       date_trunc('day', NOW() AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'Asia/Ho_Chi_Minh'
           - (d || ' days')::interval + interval '9 hours' + (j || ' hours')::interval,
       LEAST(NOW() - interval '5 min',
             date_trunc('day', NOW() AT TIME ZONE 'Asia/Ho_Chi_Minh') AT TIME ZONE 'Asia/Ho_Chi_Minh'
                 - (d || ' days')::interval + interval '10 hours' + (j || ' hours')::interval),
       0, NULL, 40000 + ((d * 37 + j * 53) % 12) * 15000
FROM generate_series(0, 34) d, generate_series(0, 1) j
WHERE j = 0 OR d % 3 <> 1;

INSERT INTO orders (created_at, updated_at, bag_count, customer_id, customer_name, customer_phone,
                    order_code, intake_code, order_type, payment_status, priority, status,
                    received_at, due_at, completed_at, delivered_at, delivered_by, notify_count, last_notified_at,
                    shelf_location, subtotal, total_amount, created_by,
                    wash_mode_id, wash_mode_name, wash_mode_hours)
SELECT s.received_at, NOW(), 1, c.id, c.full_name, c.phone,
       'LD-' || to_char(s.received_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || '-' || lpad((890 + s.n)::text, 3, '0'),
       'NH-' || to_char(s.received_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYYMMDD') || '-' || lpad((890 + s.n)::text, 3, '0'),
       s.order_type, CASE WHEN s.status = 'DELIVERED' THEN 'PAID' ELSE 'UNPAID' END, 'NORMAL', s.status,
       s.received_at, s.due_at, s.completed_at, s.delivered_at,
       CASE WHEN s.delivered_at IS NOT NULL THEN 'seed_staff' END,
       s.notify_count, CASE WHEN s.notify_count > 0 THEN s.completed_at + interval '2 days' END,
       s.shelf, s.amount, s.amount, 'seed-013',
       w.id, w.name, w.completion_hours
FROM seed_orders s
JOIN wash_modes w ON w.id = CASE WHEN s.order_type = 'EXPRESS'
    THEN (SELECT id FROM wash_modes ORDER BY completion_hours, id LIMIT 1)
    ELSE (SELECT id FROM wash_modes WHERE is_default LIMIT 1) END
JOIN customers c ON c.customer_code = 'KH99' || lpad((1 + s.n % 10)::text, 2, '0');

-- Một dòng đồ cho mỗi đơn đã phân loại
INSERT INTO order_items (created_at, updated_at, item_type, unit, quantity, unit_price, line_total,
                         express_surcharge, service_id, order_id)
SELECT o.received_at, NOW(), 'Quần áo', 'KG', s.amount / 15000, 15000, s.amount,
       CASE WHEN s.order_type = 'EXPRESS' THEN 0 END, 1, o.id
FROM seed_orders s
JOIN orders o ON o.created_by = 'seed-013'
             AND o.order_code LIKE '%-' || lpad((890 + s.n)::text, 3, '0')
             AND o.received_at = s.received_at
WHERE s.classified;

-- Thanh toán của đơn đã giao, xen kẽ tiền mặt / chuyển khoản
INSERT INTO payments (amount, confirmed_by, created_at, invoice_no, method, order_id, paid_at, status)
SELECT o.total_amount, u.id, o.delivered_at, 'SEED013-' || o.id,
       CASE WHEN o.id % 3 = 0 THEN 'BANK_TRANSFER' ELSE 'CASH' END,
       o.id, o.delivered_at, 'CONFIRMED'
FROM orders o
JOIN users u ON u.username = 'seed_admin'
WHERE o.created_by = 'seed-013' AND o.status = 'DELIVERED';

COMMIT;

SELECT status, count(*) FROM orders WHERE created_by = 'seed-013' GROUP BY status ORDER BY status;
