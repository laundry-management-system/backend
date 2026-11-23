-- CHỈ DEV / STAGING. KHÔNG chạy trên production.
-- 032: bộ dữ liệu tham chiếu đo hiệu năng báo cáo (SC-005): 50k khách, 100k đơn, 150k thanh toán,
-- 500k dòng order_status_history, voucher, sự cố, ca; trải đều 13 tháng gần nhất.
-- Điều kiện: đã chạy migrate-032-report-dashboards.sql, và đã có user seed_admin (seed-dev-dashboard-worklist.sql).
-- Chạy: psql "$DB_URL" -v ON_ERROR_STOP=1 -f backend/scripts/seed-perf-032-reference.sql ; rồi ANALYZE;
-- Chạy lại được: xóa dữ liệu cũ đánh dấu 'seed-perf-032' trước khi sinh.

BEGIN;

DELETE FROM incidents WHERE created_by = 'seed-perf-032';
DELETE FROM payments WHERE invoice_no LIKE 'SP032-%';
DELETE FROM order_status_history WHERE performed_by = 'seed-perf-032';
DELETE FROM orders WHERE created_by = 'seed-perf-032';
DELETE FROM customers WHERE created_by = 'seed-perf-032';
DELETE FROM vouchers WHERE created_by = 'seed-perf-032';
DELETE FROM shifts WHERE code LIKE 'SP032-%';

-- 50k khách
INSERT INTO customers (customer_code, full_name, phone, is_vip, status, created_by, created_at, updated_at)
SELECT 'KP' || lpad(n::text, 8, '0'), 'Khách perf ' || n, '08' || lpad(n::text, 8, '0'), n % 50 = 0, 'ACTIVE',
       'seed-perf-032', now() - interval '400 days', now()
FROM generate_series(1, 50000) n;

-- 20 voucher
INSERT INTO vouchers (code, discount_type, discount_value, start_date, end_date, min_order_value, max_uses, used_count,
                      status, created_by, created_at, updated_at)
SELECT 'SP032V' || lpad(n::text, 2, '0'), 'PERCENT', 5 + n % 10, current_date - 400, current_date + 30, 0, 100000, 0,
       'ACTIVE', 'seed-perf-032', now(), now()
FROM generate_series(1, 20) n;

-- 100k đơn: nhận trải đều 13 tháng (~395 ngày); 4% hủy, 10% ONLINE, 15% có voucher, 25% khách vãng lai
CREATE TEMP TABLE sp_orders ON COMMIT DROP AS
SELECT n,
       now() - (random() * 395 || ' days')::interval AS received_at,
       CASE WHEN n % 25 = 0 THEN 'CANCELLED' WHEN n % 5 = 0 THEN 'RECEIVED' WHEN n % 7 = 0 THEN 'PROCESSING' ELSE 'DELIVERED' END AS status,
       (40000 + (n % 30) * 10000)::numeric AS amount
FROM generate_series(1, 100000) n;

INSERT INTO orders (created_at, updated_at, bag_count, customer_id, customer_name, customer_phone, order_code, intake_code,
                    order_type, payment_status, priority, status, received_at, due_at, completed_at, delivered_at,
                    notify_count, subtotal, total_amount, service_type, created_by, channel, voucher_id, voucher_code)
SELECT s.received_at, now(), 1, c.id, COALESCE(c.full_name, 'Khách lẻ'), COALESCE(c.phone, '0900000000'), 'LD-P' || lpad(s.n::text, 8, '0'), 'NH-P' || lpad(s.n::text, 8, '0'),
       'NORMAL', CASE WHEN s.status = 'DELIVERED' THEN 'PAID' ELSE 'UNPAID' END, 'NORMAL', s.status,
       s.received_at, s.received_at + interval '2 days',
       CASE WHEN s.status = 'DELIVERED' THEN s.received_at + interval '1 day' END,
       CASE WHEN s.status = 'DELIVERED' THEN s.received_at + interval '2 days' END,
       0, s.amount, s.amount, 'WASH_DRY', 'seed-perf-032', CASE WHEN s.n % 10 = 3 THEN 'ONLINE' ELSE 'OFFLINE' END,
       v.id, v.code
FROM sp_orders s
LEFT JOIN customers c ON s.n % 4 <> 0 AND c.customer_code = 'KP' || lpad((1 + s.n % 50000)::text, 8, '0')
LEFT JOIN vouchers v ON s.n % 7 = 1 AND v.code = 'SP032V' || lpad((1 + s.n % 20)::text, 2, '0');

-- 150k thanh toán: mỗi đơn đã giao 1 khoản, thêm khoản thứ hai cho 50k đơn (trả hai lần), 5% PENDING
INSERT INTO payments (amount, confirmed_by, created_at, invoice_no, method, order_id, paid_at, status)
SELECT o.total_amount / k.part, u.id, o.delivered_at + (k.part - 1) * interval '1 hour',
       'SP032-' || o.id || '-' || k.part, CASE WHEN o.id % 3 = 0 THEN 'BANK_TRANSFER' ELSE 'CASH' END,
       o.id, o.delivered_at + (k.part - 1) * interval '1 hour', CASE WHEN o.id % 20 = 0 THEN 'PENDING' ELSE 'CONFIRMED' END
FROM orders o
JOIN users u ON u.username = 'seed_admin'
JOIN LATERAL generate_series(1, CASE WHEN o.id % 2 = 0 THEN 2 ELSE 1 END) AS k(part) ON true
WHERE o.created_by = 'seed-perf-032' AND o.status = 'DELIVERED';

-- 500k dòng lịch sử: CREATED + STATUS_CHANGE → COMPLETED / DELIVERED, đơn hủy có CANCELLED (reason_code lẫn NULL),
-- phần còn lại là ITEMS_UPDATED / PRICE_ADJUST để đủ khối lượng
INSERT INTO order_status_history (created_at, updated_at, order_id, action_type, old_value, new_value, reason, reason_code, performed_by)
SELECT o.received_at + h.off, now(), o.id, h.action, h.old_v, h.new_v,
       CASE WHEN h.action = 'CANCELLED' THEN 'Lý do hủy' END,
       CASE WHEN h.action = 'CANCELLED' AND o.id % 3 <> 0 THEN (ARRAY['CUSTOMER_CHANGED_MIND','CUSTOMER_UNREACHABLE','WRONG_ORDER_INFO','OUT_OF_SERVICE_AREA','DUPLICATE_ORDER','OTHER'])[1 + o.id % 6] END,
       'seed-perf-032'
FROM orders o
JOIN LATERAL (VALUES
    (interval '0',        'CREATED',       NULL,         'RECEIVED',  true),
    (interval '1 hour',   'ITEMS_UPDATED', NULL,         NULL,        true),
    (interval '2 hours',  'PRICE_ADJUST',  NULL,         NULL,        true),
    (interval '3 hours',  'ITEMS_UPDATED', NULL,         NULL,        true),
    (interval '1 day',    'STATUS_CHANGE', 'PROCESSING', 'COMPLETED', o.status = 'DELIVERED'),
    (interval '2 days',   'STATUS_CHANGE', 'COMPLETED',  'DELIVERED', o.status = 'DELIVERED'),
    (interval '4 hours',  'CANCELLED',     'RECEIVED',   'CANCELLED', o.status = 'CANCELLED')
) AS h(off, action, old_v, new_v, ok) ON h.ok
WHERE o.created_by = 'seed-perf-032';

-- 3000 sự cố
INSERT INTO incidents (created_at, updated_at, code, order_id, type, description, amount, max_compensation, affected_count,
                       status, created_by, deducted_amount, payout_due)
SELECT o.received_at + interval '1 day', now(), 'SP' || lpad(o.id::text, 8, '0'), o.id,
       (ARRAY['LOST','TORN','FADED','SHRUNK','OTHER'])[1 + o.id % 5], 'Sự cố perf', 50000, 100000, 1,
       (ARRAY['PENDING','APPROVED','REJECTED'])[1 + o.id % 3], 'seed-perf-032', 0, 0
FROM orders o
WHERE o.created_by = 'seed-perf-032' AND o.status = 'DELIVERED' AND o.id % 25 = 1
LIMIT 3000;

-- 400 ca đóng, một ca mỗi ngày
INSERT INTO shifts (code, status, opened_by, opened_at, opening_cash, closed_by, closed_at, cash_in, cash_in_count, cash_out,
                    cash_out_count, expected_cash, counted_cash, difference, transfer_total, transfer_count, bank_reconciled,
                    keep_cash, hand_in_cash)
SELECT 'SP032-' || lpad(d::text, 4, '0'), 'CLOSED', u.id, now() - (d || ' days')::interval, 500000,
       u.id, now() - (d || ' days')::interval + interval '8 hours', 800000, 8, 0, 0, 1300000, 1300000, 0, 400000, 4, true,
       500000, 800000
FROM generate_series(1, 400) d
JOIN users u ON u.username = 'seed_admin';

COMMIT;

ANALYZE;
