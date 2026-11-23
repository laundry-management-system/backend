-- Seed dev (spec 031, FR-039): 4 đơn ONLINE đã xác nhận, ĐÃ QUÁ giờ hẹn 2 giờ: 01/02 khách tự mang đồ tới (AT_STORE), 03/04 tiệm đi lấy (PICKUP).
-- Chạy: docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/seed-dev-late-dropoff.sql
-- Chạy lại được (xóa các đơn LD-SEED-LATE-* rồi tạo lại, mốc thời gian tính theo lúc chạy). Cần đã chạy seed-dev-dashboard-worklist.sql (khách KH9950 "Seed Khách hàng").
--   LD-SEED-LATE-01: Giặt nhanh 6h, hẹn = now-2h, hoàn thành = now+2h (< now+6h) -> bấm "Khách đã tới đưa đồ" sẽ lùi hoàn thành theo dueAtFor(now, 6h)
--   LD-SEED-LATE-02: Giặt nhanh 6h, hẹn = now-2h, hoàn thành = 17:00 sau 2 ngày (> now+6h) -> giữ nguyên
--   LD-SEED-LATE-03: như 01 nhưng PICKUP (Chờ lấy đồ) -> bấm "Đã lấy đồ từ khách" sẽ lùi hoàn thành, ghi chú "Tiệm tới nhận trễ giờ hẹn"
--   LD-SEED-LATE-04: như 02 nhưng PICKUP -> giữ nguyên
BEGIN;

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

COMMIT;
