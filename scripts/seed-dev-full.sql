-- SEED ĐẦY ĐỦ cho DB dev: một file, mọi bảng nghiệp vụ đều có dữ liệu để người mới clone về là xem được.
-- CHỈ dùng cho DB dev. KHÔNG chạy trên production.
--
-- Điều kiện: BE đã khởi động ít nhất 1 lần (Hibernate tạo bảng, data.sql nạp admin, bảng giá DV01–DV10,
-- hình thức giặt, settings mặc định).
--
-- Chạy (từ thư mục gốc repo):  bash backend/scripts/seed-all.sh
--   (script copy ảnh mẫu trong backend/scripts/seed-assets/ vào backend/uploads/ rồi chạy file này)
-- Hoặc chỉ SQL:  docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/seed-dev-full.sql
--
-- Chạy lại bao nhiêu lần cũng được: xóa dữ liệu seed cũ rồi tạo lại, mốc thời gian tính theo lúc chạy.
-- LƯU Ý: phần Ca làm việc XÓA MỌI CA hiện có (giống seed-dev-shifts.sql).
--
-- Tài khoản (mật khẩu chung): Seed013-fcf0d0f7
--   seed_admin  (Admin)           seed_staff, seed_staff2 (Nhân viên)     seed_staff_off (Nhân viên đã nghỉ, không đăng nhập được)
--   Khách (trang công khai /dat-don): SĐT 0999990011 hoặc seed-customer@example.test
--
-- Gộp từ: seed-dev-dashboard-worklist.sql, seed-dev-customer.sql, seed-dev-service-areas.sql,
--         seed-dev-late-dropoff.sql, seed-dev-shifts.sql  (+ phần mới ở cuối file)

BEGIN;

-- Dọn các bảng phụ thuộc vào đơn seed trước, để phần xóa đơn seed bên dưới không vướng khóa ngoại
DELETE FROM incident_images WHERE incident_id IN (SELECT id FROM incidents WHERE code LIKE 'SC-SEED-%');
DELETE FROM incidents WHERE code LIKE 'SC-SEED-%';
DELETE FROM order_images WHERE document_id IN (SELECT id FROM documents WHERE storage_key LIKE 'seed-%');
-- Audit seed đánh dấu bằng ip 127.0.0.2
DELETE FROM audit_logs WHERE ip_address = '127.0.0.2';

-- =====================================================================
-- Nhân viên, khách, đơn, thanh toán  (copy từ seed-dev-dashboard-worklist.sql)
-- =====================================================================

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



-- =====================================================================
-- Tài khoản khách cho trang công khai  (copy từ seed-dev-customer.sql)
-- =====================================================================
INSERT INTO customers (customer_code, full_name, phone, email, address, is_vip, status, source, password_hash, password_changed_at, created_by, created_at, updated_at)
VALUES ('KH9950', 'Seed Khách hàng', '0999990011', 'seed-customer@example.test', 'Phường Bến Nghé, Quận 1', false, 'ACTIVE', 'SELF_REGISTERED',
        '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa', NOW(), 'seed-customer', NOW(), NOW())
ON CONFLICT (phone) DO UPDATE SET password_hash = EXCLUDED.password_hash, status = 'ACTIVE', email = EXCLUDED.email, updated_at = NOW();

-- =====================================================================
-- Khu vực giao nhận  (copy từ seed-dev-service-areas.sql)
-- =====================================================================

INSERT INTO service_areas (name, pickup_fee, delivery_fee, active, created_at, updated_at)
VALUES ('Phường Bến Thành', 0, 0, TRUE, NOW(), NOW()),
       ('Phường Sài Gòn', 10000, 10000, TRUE, NOW(), NOW()),
       ('Phường Tân Định', 10000, 10000, TRUE, NOW(), NOW()),
       ('Phường Bàn Cờ', 15000, 15000, TRUE, NOW(), NOW()),
       ('Phường Xuân Hòa', 15000, 20000, TRUE, NOW(), NOW()),
       ('Phường Nhiêu Lộc', 20000, 20000, TRUE, NOW(), NOW()),
       ('Phường Phú Nhuận', 20000, 20000, TRUE, NOW(), NOW()),
       ('Phường Gia Định', 25000, 25000, TRUE, NOW(), NOW()),
       ('Phường Thạnh Mỹ Tây', 25000, 25000, TRUE, NOW(), NOW()),
       ('Phường An Khánh', 35000, 35000, TRUE, NOW(), NOW()),
       ('Phường Tân Sơn Nhất', 30000, 30000, FALSE, NOW(), NOW()),
       ('Phường Thủ Đức', 40000, 40000, FALSE, NOW(), NOW())
ON CONFLICT (lower(name)) DO UPDATE
SET pickup_fee = EXCLUDED.pickup_fee,
    delivery_fee = EXCLUDED.delivery_fee,
    active = EXCLUDED.active,
    updated_at = NOW();


-- =====================================================================
-- Đơn online trễ giờ hẹn  (copy từ seed-dev-late-dropoff.sql)
-- =====================================================================

DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE order_code LIKE 'LD-SEED-LATE-%');
DELETE FROM order_fulfillments WHERE order_id IN (SELECT id FROM orders WHERE order_code LIKE 'LD-SEED-LATE-%');
DELETE FROM orders WHERE order_code LIKE 'LD-SEED-LATE-%';

CREATE TEMP TABLE ld (code text, svc bigint, price numeric, due timestamptz, method text) ON COMMIT DROP;
INSERT INTO ld VALUES
 ('LD-SEED-LATE-01', 1, 40000, now() + interval '2 hours', 'AT_STORE'),
 ('LD-SEED-LATE-02', 2, 60000, ((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + 2 + time '17:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'AT_STORE'),
 ('LD-SEED-LATE-03', 1, 40000, now() + interval '2 hours', 'PICKUP'),
 ('LD-SEED-LATE-04', 2, 60000, ((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + 2 + time '17:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 'PICKUP');

INSERT INTO orders (created_at, updated_at, bag_count, created_by, customer_id, customer_name, customer_phone, due_at, notify_count,
                    order_code, order_type, payment_status, priority, status, subtotal, total_amount, intake_code, intake_request_id,
                    requested_service_ids, requested_service_prices, wash_whites_separately, whites_fee, incident_discount, channel,
                    confirmed_at, confirmed_by, wash_mode_id, wash_mode_name, wash_mode_hours)
SELECT now() - interval '3 hours', now() - interval '3 hours', 1, 'KH9950', c.id, c.full_name, c.phone, ld.due, 0,
       ld.code, 'EXPRESS', 'UNPAID', 'NORMAL', 'AWAITING_DROPOFF', 0, 0, 'NH-' || ld.code, gen_random_uuid(),
       jsonb_build_array(ld.svc), jsonb_build_object(ld.svc::text, ld.price), false, 0, 0, 'ONLINE',
       now() - interval '2 hours 30 minutes', 'seed_admin', 2, 'Giặt nhanh', 6
FROM ld, customers c WHERE c.customer_code = 'KH9950';

INSERT INTO order_fulfillments (created_at, updated_at, appointment_at, contact_name, contact_phone, dropoff_method, dropoff_status,
                                delivery_fee, pickup_fee, return_method, terms_accepted_at, order_id, pickup_address, pickup_area_id, pickup_area_name)
SELECT now(), now(), now() - interval '2 hours', o.customer_name, o.customer_phone, ld.method,
       CASE ld.method WHEN 'PICKUP' THEN 'AWAITING_PICKUP' ELSE 'AWAITING_CUSTOMER_DROPOFF' END,
       0, 0, 'AT_STORE', now() - interval '3 hours', o.id,
       CASE ld.method WHEN 'PICKUP' THEN '12 Đường Seed' END, a.id, a.name
FROM orders o JOIN ld ON ld.code = o.order_code
LEFT JOIN service_areas a ON ld.method = 'PICKUP' AND a.id = (SELECT min(id) FROM service_areas);

INSERT INTO order_status_history (created_at, updated_at, action_type, new_value, performed_by, reason, order_id)
SELECT now() - interval '3 hours', now() - interval '3 hours', 'CREATED', 'PENDING_CONFIRMATION', 'KH9950', 'Đặt online (seed)', id
FROM orders WHERE order_code LIKE 'LD-SEED-LATE-%';
INSERT INTO order_status_history (created_at, updated_at, action_type, old_value, new_value, performed_by, order_id)
SELECT now() - interval '2 hours 30 minutes', now() - interval '2 hours 30 minutes', 'STATUS_CHANGE', 'PENDING_CONFIRMATION', 'AWAITING_DROPOFF', 'seed_admin', id
FROM orders WHERE order_code LIKE 'LD-SEED-LATE-%';


-- =====================================================================
-- Ca làm việc (XÓA mọi ca hiện có)  (copy từ seed-dev-shifts.sql)
-- =====================================================================

DELETE FROM audit_logs WHERE entity_type = 'SHIFT';
UPDATE payments SET shift_id = NULL, resolved_shift_id = NULL
 WHERE shift_id IS NOT NULL OR resolved_shift_id IS NOT NULL;
DELETE FROM shifts;

-- Lịch sử vài ngày cho màn Đối chiếu tiền cuối ca: đã duyệt / chờ duyệt, thiếu / dư / khớp, lệch chuyển khoản
INSERT INTO shifts (code, status, opened_by, opened_at, opening_cash, closed_by, closed_at,
                    cash_in, cash_in_count, cash_out, cash_out_count, expected_cash, counted_cash, difference,
                    difference_reason, transfer_total, transfer_count, bank_reconciled, bank_total,
                    transfer_difference, transfer_difference_reason, keep_cash, hand_in_cash,
                    approved_by, approved_at, approval_note)
SELECT 'CA-' || to_char(day.d - h.days_ago, 'YYYYMMDD') || '-' || h.seq, h.status, staff.id,
       (day.d - h.days_ago + h.open_at) AT TIME ZONE 'Asia/Ho_Chi_Minh', 500000,
       staff.id, (day.d - h.days_ago + h.close_at) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       h.cash_in, h.cash_in_n, 0, 0, 500000 + h.cash_in, 500000 + h.cash_in + h.diff, h.diff,
       h.diff_reason, h.transfer, h.transfer_n, true, h.transfer + h.transfer_diff,
       h.transfer_diff, h.transfer_reason, 500000, h.cash_in + h.diff,
       CASE WHEN h.status = 'APPROVED' THEN admin.id END,
       CASE WHEN h.status = 'APPROVED' THEN (day.d - h.days_ago + h.close_at + interval '1 hour') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
       h.note
  FROM (VALUES
          (3, '01', time '07:05', time '14:00', 'APPROVED', 480000, 6, 0, NULL, 390000, 3, 0, NULL, NULL),
          (3, '02', time '14:01', time '21:10', 'APPROVED', 590000, 7, 5000, 'Khách để lại tiền thừa, chưa kịp trả', 475000, 4, 0, NULL, 'Đã ghi nhận tiền dư'),
          (2, '01', time '07:00', time '14:02', 'CLOSED', 820000, 8, -15000, 'Thối nhầm cho khách đơn LD-…', 610000, 5, 0, NULL, NULL),
          (2, '02', time '14:00', time '21:05', 'CLOSED', 700000, 6, 0, NULL, 530000, 4, -22000, 'Khách chuyển thiếu 22.000đ, đã báo Admin', NULL),
          (1, '01', time '07:02', time '13:58', 'CLOSED', 640000, 5, -20000, 'Thối nhầm 20.000đ', 280000, 2, 0, NULL, NULL)
       ) AS h(days_ago, seq, open_at, close_at, status, cash_in, cash_in_n, diff, diff_reason,
              transfer, transfer_n, transfer_diff, transfer_reason, note),
       (SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS d) day,
       (SELECT id FROM users WHERE username = 'seed_staff') staff,
       (SELECT id FROM users WHERE username = 'seed_admin') admin;


INSERT INTO shifts (code, status, opened_by, opened_at, opening_cash, closed_by, closed_at,
                    cash_in, cash_in_count, cash_out, cash_out_count, expected_cash, counted_cash, difference,
                    transfer_total, transfer_count, bank_reconciled, keep_cash, hand_in_cash)
SELECT 'CA-' || to_char(d, 'YYYYMMDD') || '-02', 'CLOSED', u.id,
       (d + time '14:00') AT TIME ZONE 'Asia/Ho_Chi_Minh', 500000,
       u.id, (d + time '21:04') AT TIME ZONE 'Asia/Ho_Chi_Minh',
       750000, 5, 0, 0, 1250000, 1250000, 0, 530000, 3, true, 500000, 750000
  FROM users u, (SELECT (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - 1 AS d) day
 WHERE u.username = 'seed_staff';



-- =====================================================================
-- PHẦN MỚI: các bảng chưa có trong seed cũ
-- =====================================================================

-- Khách seed-013 tạo tại quầy
UPDATE customers SET source = 'COUNTER' WHERE created_by = 'seed-013' AND source IS NULL;

-- Dịch vụ khách yêu cầu lúc nhận đồ (phiếu nhận đồ / In phiếu đọc từ đây): DV01, giá hình thức của đơn
UPDATE orders o SET requested_service_ids = jsonb_build_array(s.id),
                    requested_service_prices = jsonb_build_object(s.id::text, coalesce(p.price, 15000))
FROM services s LEFT JOIN service_wash_modes p ON p.service_id = s.id
WHERE s.service_code = 'DV01' AND o.created_by = 'seed-013' AND (p.wash_mode_id = o.wash_mode_id OR p.id IS NULL);

-- Lịch sử trạng thái cho đơn seed-013 (màn Chi tiết đơn có timeline)
INSERT INTO order_status_history (created_at, updated_at, action_type, new_value, performed_by, reason, order_id)
SELECT o.received_at, o.received_at, 'CREATED', 'RECEIVED', 'seed_staff', 'Nhận đồ tại quầy (seed)', o.id
FROM orders o WHERE o.created_by = 'seed-013';
INSERT INTO order_status_history (created_at, updated_at, action_type, old_value, new_value, performed_by, order_id)
SELECT o.received_at + interval '1 hour', o.received_at + interval '1 hour', 'STATUS_CHANGE', 'RECEIVED', 'PROCESSING', 'seed_staff', o.id
FROM orders o WHERE o.created_by = 'seed-013' AND o.status IN ('PROCESSING', 'COMPLETED', 'DELIVERED');
INSERT INTO order_status_history (created_at, updated_at, action_type, old_value, new_value, performed_by, order_id)
SELECT o.completed_at, o.completed_at, 'STATUS_CHANGE', 'PROCESSING', 'COMPLETED', 'seed_staff', o.id
FROM orders o WHERE o.created_by = 'seed-013' AND o.status IN ('COMPLETED', 'DELIVERED');
INSERT INTO order_status_history (created_at, updated_at, action_type, old_value, new_value, performed_by, order_id)
SELECT o.delivered_at, o.delivered_at, 'STATUS_CHANGE', 'COMPLETED', 'DELIVERED', 'seed_staff', o.id
FROM orders o WHERE o.created_by = 'seed-013' AND o.status = 'DELIVERED';
INSERT INTO order_status_history (created_at, updated_at, action_type, old_value, new_value, performed_by, reason, order_id)
SELECT o.received_at + interval '30 min', o.received_at + interval '30 min', 'CANCELLED', 'RECEIVED', 'CANCELLED', 'seed_admin', 'Khách đổi ý (seed)', o.id
FROM orders o WHERE o.created_by = 'seed-013' AND o.status = 'CANCELLED';

-- Thêm nhân viên: 1 đang làm, 1 đã nghỉ
INSERT INTO users (employee_code, full_name, phone, email, username, password_hash, role, status, hired_date, created_at, updated_at)
VALUES ('NV9903', 'Seed Nhân viên 2', '0999990003', 'seed-staff2@example.test', 'seed_staff2', '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa', 'STAFF', 'ACTIVE', CURRENT_DATE - 90, NOW(), NOW()),
       ('NV9904', 'Seed Nhân viên nghỉ', '0999990004', 'seed-staff-off@example.test', 'seed_staff_off', '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa', 'STAFF', 'INACTIVE', CURRENT_DATE - 300, NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash, status = EXCLUDED.status, updated_at = NOW();

-- Thông tin cửa hàng: chỉ ghi đè giá trị mặc định của data.sql (chưa ai sửa)
UPDATE settings s SET setting_value = v.val, updated_at = NOW()
FROM (VALUES ('STORE_NAME', 'Tiệm Giặt Sấy Seed'),
             ('STORE_ADDRESS', '12 Lê Lợi, Phường Bến Thành, TP.HCM'),
             ('STORE_PHONE', '0909123456'),
             ('BANK_ACCOUNT_NO', '0123456789'),
             ('BANK_ACCOUNT_NAME', 'TIEM GIAT SAY SEED')) AS v(key, val)
WHERE s.setting_key = v.key AND s.updated_by IS NULL;

-- Voucher: đang chạy (%), đang chạy (tiền, có đơn tối thiểu), hết hạn, đã tắt
INSERT INTO vouchers (code, discount_type, discount_value, start_date, end_date, min_order_value, max_uses, used_count, status, created_by, created_at, updated_at)
VALUES ('SEED10',     'PERCENT', 10,    CURRENT_DATE - 7,  CURRENT_DATE + 30, 0,      100, 3, 'ACTIVE',   'seed_admin', NOW(), NOW()),
       ('SEED20K',    'AMOUNT',  20000, CURRENT_DATE - 7,  CURRENT_DATE + 30, 100000, 50,  0, 'ACTIVE',   'seed_admin', NOW(), NOW()),
       ('SEEDHETHAN', 'PERCENT', 15,    CURRENT_DATE - 60, CURRENT_DATE - 1,  0,      10,  10, 'ACTIVE',  'seed_admin', NOW(), NOW()),
       ('SEEDTAT',    'AMOUNT',  10000, CURRENT_DATE - 7,  CURRENT_DATE + 30, 0,      10,  0, 'DISABLED', 'seed_admin', NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET discount_type = EXCLUDED.discount_type, discount_value = EXCLUDED.discount_value,
    start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date, min_order_value = EXCLUDED.min_order_value,
    max_uses = EXCLUDED.max_uses, used_count = EXCLUDED.used_count, status = EXCLUDED.status, updated_at = NOW();

-- File ảnh mẫu (file thật nằm ở backend/scripts/seed-assets/, seed-all.sh copy sang backend/uploads/)
INSERT INTO documents (content_type, original_name, size_bytes, storage_key, uploaded_by, created_at, updated_at)
VALUES ('image/png', 'may-giat.png',  4348, 'seed-machine-washer.png', 'seed_admin', NOW(), NOW()),
       ('image/png', 'may-say.png',   4290, 'seed-machine-dryer.png',  'seed_admin', NOW(), NOW()),
       ('image/png', 'do-khach.png',  6800, 'seed-order-item.png',     'seed_staff', NOW(), NOW()),
       ('image/png', 'su-co.png',     4634, 'seed-incident.png',       'seed_staff', NOW(), NOW())
ON CONFLICT (storage_key) DO NOTHING;

-- Máy giặt / máy sấy: đủ 3 trạng thái. Mã theo mẫu của MachineCode (W/D-hãng-số). Có rồi thì giữ nguyên.
INSERT INTO machines (machine_code, name, type, capacity_kg, status, brand, model, serial_number, location, note,
                      purchase_date, warranty_until, image_document_id, created_at, updated_at)
SELECT v.code, v.name, v.type, v.kg, v.status, v.brand, v.model, v.serial, v.loc, v.note,
       CURRENT_DATE - v.age, CURRENT_DATE - v.age + 730, d.id, NOW(), NOW()
FROM (VALUES
    ('W-LG-001',  'Máy giặt LG 01',         'WASHER', 10.0, 'READY',       'LG',          'FV1410S4P', 'SEED-W-LG-001',  'Dãy trái',  NULL, 400),
    ('W-LG-002',  'Máy giặt LG 02',         'WASHER', 10.0, 'READY',       'LG',          'FV1410S4P', 'SEED-W-LG-002',  'Dãy trái',  NULL, 400),
    ('W-ELE-001', 'Máy giặt Electrolux 01', 'WASHER', 12.0, 'READY',       'Electrolux',  'EWF1242',   'SEED-W-ELE-001', 'Dãy phải',  NULL, 200),
    ('W-LG-003',  'Máy giặt LG 03',         'WASHER', 15.0, 'MAINTENANCE', 'LG',          'TWINWash',  'SEED-W-LG-003',  'Dãy phải',  'Kêu to khi vắt, chờ thợ', 900),
    ('D-LG-001',  'Máy sấy LG 01',          'DRYER',  9.0,  'READY',       'LG',          'DVHP09W',   'SEED-D-LG-001',  'Góc trong', NULL, 400),
    ('D-ELE-001', 'Máy sấy Electrolux 01',  'DRYER',  8.0,  'READY',       'Electrolux',  'EDV805',    'SEED-D-ELE-001', 'Góc trong', NULL, 200),
    ('D-ELE-002', 'Máy sấy Electrolux 02',  'DRYER',  8.0,  'RETIRED',     'Electrolux',  'EDV705',    'SEED-D-ELE-002', 'Kho',       'Hỏng mô-tơ, đã thanh lý', 1500)
) AS v(code, name, type, kg, status, brand, model, serial, loc, note, age)
JOIN documents d ON d.storage_key = CASE v.type WHEN 'WASHER' THEN 'seed-machine-washer.png' ELSE 'seed-machine-dryer.png' END
ON CONFLICT DO NOTHING;

-- Ảnh đồ của khách cho 2 đơn seed (Mới nhận đã phân loại #3, Đang xử lý #6)
INSERT INTO order_images (image_url, note, order_item_id, document_id, created_at, updated_at)
SELECT '/api/documents/files/' || d.storage_key, 'Ảnh lúc nhận đồ (seed)', i.id, d.id, NOW(), NOW()
FROM orders o
JOIN order_items i ON i.order_id = o.id
JOIN documents d ON d.storage_key = 'seed-order-item.png'
WHERE o.created_by = 'seed-013' AND (o.order_code LIKE '%-893' OR o.order_code LIKE '%-896');

-- Sự cố: chờ duyệt, đã duyệt bồi thường (đã chi), từ chối, đã duyệt giặt lại
-- Hệ số K = 10, đơn giá 15.000đ → mức bồi thường tối đa 150.000đ
CREATE TEMP TABLE seed_incidents (code text, suffix text, type text, status text, resolution text, amount numeric,
                                  description text, review_note text, paid boolean) ON COMMIT DROP;
INSERT INTO seed_incidents VALUES
 ('SC-SEED-01', '896', 'TORN',   'PENDING',  'COMPENSATION', 100000, 'Áo sơ mi trắng rách đường may ở vai',          NULL,                          false),
 ('SC-SEED-02', '934', 'LOST',   'APPROVED', 'COMPENSATION', 120000, 'Khách báo thiếu 1 áo thun khi nhận đồ',        'Đã kiểm camera, xác nhận mất', true),
 ('SC-SEED-03', '936', 'FADED',  'REJECTED', 'DISCOUNT',     50000,  'Quần jean bị phai màu',                        'Đồ đã phai trước khi gửi',     false),
 ('SC-SEED-04', '923', 'SHRUNK', 'APPROVED', 'REWASH',       0,      'Áo len bị co sau khi sấy',                     'Giặt lại bằng tay, miễn phí',  false);

INSERT INTO incidents (code, order_id, order_item_id, type, status, resolution, affected_count, amount, deducted_amount,
                       payout_due, payout_amount, payout_method, payout_shift_id, paid_at, paid_by,
                       description, item_label, item_quantity, item_unit, unit_price, line_total, max_compensation,
                       review_note, reviewed_at, reviewed_by, created_by, created_at, updated_at)
SELECT s.code, o.id, i.id, s.type, s.status, s.resolution, 1, s.amount, 0,
       CASE WHEN s.status = 'APPROVED' AND s.resolution = 'COMPENSATION' THEN s.amount ELSE 0 END,
       CASE WHEN s.paid THEN s.amount END, CASE WHEN s.paid THEN 'CASH' END,
       CASE WHEN s.paid THEN (SELECT id FROM shifts ORDER BY opened_at DESC LIMIT 1) END,
       CASE WHEN s.paid THEN NOW() - interval '1 day' END, CASE WHEN s.paid THEN 'seed_admin' END,
       s.description, 'Giặt sấy quần áo', i.quantity, 'KG', i.unit_price, i.line_total, LEAST(10 * i.unit_price, 500000),
       s.review_note, CASE WHEN s.status <> 'PENDING' THEN NOW() - interval '1 day' END,
       CASE WHEN s.status <> 'PENDING' THEN 'seed_admin' END,
       'seed_staff', NOW() - interval '2 days', NOW()
FROM seed_incidents s
JOIN orders o ON o.created_by = 'seed-013' AND o.order_code LIKE '%-' || s.suffix
JOIN order_items i ON i.order_id = o.id;

INSERT INTO incident_images (image_url, document_id, incident_id, created_at, updated_at)
SELECT '/api/documents/files/' || d.storage_key, d.id, x.id, NOW(), NOW()
FROM incidents x JOIN documents d ON d.storage_key = 'seed-incident.png'
WHERE x.code IN ('SC-SEED-01', 'SC-SEED-02');

-- Yêu cầu đăng ký tài khoản khách: chờ duyệt, đã kích hoạt, bị từ chối
DELETE FROM customer_registrations WHERE phone IN ('0999990021', '0999990011', '0999990023');
-- Đăng ký online tạo hồ sơ khách ngay (chưa có mật khẩu); màn Khách hàng lọc "Chờ duyệt" theo hồ sơ này
INSERT INTO customers (customer_code, full_name, phone, email, is_vip, status, source, created_by, created_at, updated_at)
VALUES ('KH9951', 'Nguyễn Chờ Duyệt', '0999990021', NULL, false, 'ACTIVE', 'SELF_REGISTERED', 'seed-full', NOW() - interval '3 hours', NOW())
ON CONFLICT (phone) DO NOTHING;
INSERT INTO customer_registrations (full_name, phone, email, password_hash, status, customer_id, reviewed_at, reviewed_by, created_at, updated_at)
VALUES ('Nguyễn Chờ Duyệt', '0999990021', 'seed-pending@example.test', '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa',
        'PENDING', (SELECT id FROM customers WHERE phone = '0999990021'), NULL, NULL, NOW() - interval '3 hours', NOW()),
       ('Seed Khách hàng', '0999990011', 'seed-customer@example.test', '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa',
        'ACTIVATED', (SELECT id FROM customers WHERE phone = '0999990011'), NOW() - interval '5 days', 'seed_admin', NOW() - interval '6 days', NOW()),
       ('Trần Bị Từ Chối', '0999990023', NULL, '$2a$10$AogH7MTOKKNHoQGbgEzolOdiF..A6OI/xqL1gPkFwQ3e4p10cxXVa',
        'REJECTED', NULL, NOW() - interval '1 day', 'seed_admin', NOW() - interval '2 days', NOW());

-- Nhật ký hệ thống
INSERT INTO audit_logs (action, entity_type, entity_id, user_id, old_data, new_data, reason, ip_address, created_at, updated_at)
SELECT 'MACHINE_STATUS', 'MACHINE', m.id, u.id, '{"status": "READY"}', '{"status": "MAINTENANCE"}', 'Kêu to khi vắt', '127.0.0.2', NOW() - interval '3 days', NOW() - interval '3 days'
FROM machines m, users u WHERE m.machine_code = 'W-LG-003' AND u.username = 'seed_staff'
UNION ALL
SELECT 'PRICE_CHANGE', 'SERVICE', s.id, u.id, jsonb_build_object('modePrice:' || p.wash_mode_id, p.price - 2000), jsonb_build_object('modePrice:' || p.wash_mode_id, p.price), 'Điều chỉnh giá theo thị trường', '127.0.0.2', NOW() - interval '10 days', NOW() - interval '10 days'
FROM services s JOIN service_wash_modes p ON p.service_id = s.id JOIN wash_modes w ON w.id = p.wash_mode_id AND w.is_default, users u
WHERE s.service_code = 'DV01' AND u.username = 'seed_admin'
UNION ALL
SELECT 'INCIDENT_APPROVE', 'INCIDENT', x.id, u.id,
       jsonb_build_object('code', x.code, 'amount', x.amount, 'status', 'PENDING', 'resolution', x.resolution),
       jsonb_build_object('code', x.code, 'amount', x.amount, 'status', x.status, 'payoutDue', x.payout_due, 'resolution', x.resolution, 'deductedAmount', x.deducted_amount),
       NULL, '127.0.0.2', x.reviewed_at, x.reviewed_at
FROM incidents x, users u WHERE x.code LIKE 'SC-SEED-%' AND x.status <> 'PENDING' AND u.username = 'seed_admin'
UNION ALL
SELECT 'INCIDENT_PAYOUT', 'INCIDENT', x.id, u.id, jsonb_build_object('payoutDue', x.payout_due),
       jsonb_build_object('amount', x.payout_amount, 'method', x.payout_method, 'shiftCode', sh.code),
       NULL, '127.0.0.2', x.paid_at, x.paid_at
FROM incidents x JOIN shifts sh ON sh.id = x.payout_shift_id, users u WHERE x.code = 'SC-SEED-02' AND u.username = 'seed_admin';

COMMIT;

SELECT 'users' AS bang, count(*) FROM users
UNION ALL SELECT 'customers', count(*) FROM customers
UNION ALL SELECT 'orders', count(*) FROM orders
UNION ALL SELECT 'payments', count(*) FROM payments
UNION ALL SELECT 'shifts', count(*) FROM shifts
UNION ALL SELECT 'service_areas', count(*) FROM service_areas
UNION ALL SELECT 'vouchers', count(*) FROM vouchers
UNION ALL SELECT 'machines', count(*) FROM machines
UNION ALL SELECT 'incidents', count(*) FROM incidents
UNION ALL SELECT 'customer_registrations', count(*) FROM customer_registrations
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs;
