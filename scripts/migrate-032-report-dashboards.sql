-- 032: báo cáo / dashboard. Thêm cột order_status_history.reason_code và 9 index phục vụ truy vấn báo cáo.
--
-- Chạy:  psql "$DB_URL" -v ON_ERROR_STOP=1 -f backend/scripts/migrate-032-report-dashboards.sql
--   KHÔNG dùng psql -1 (CREATE INDEX CONCURRENTLY không chạy trong transaction); script cũng không bọc BEGIN.
-- Chạy lại bao nhiêu lần cũng được (IF NOT EXISTS).
--
-- RUNBOOK index INVALID: nếu một lần build CONCURRENTLY thất bại, index nằm ở trạng thái INVALID và
-- IF NOT EXISTS sẽ bỏ qua nó khi chạy lại. Truy vấn cuối script liệt kê các index lỗi. Xử lý:
--   DROP INDEX CONCURRENTLY <tên>;   rồi chạy lại script.
-- Quay lại: backend/scripts/rollback-032-report-dashboards.sql

SET lock_timeout = '5s';

ALTER TABLE order_status_history ADD COLUMN IF NOT EXISTS reason_code varchar(40);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_received_at ON orders (received_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_delivered_at ON orders (delivered_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_customer_received ON orders (customer_id, received_at) WHERE customer_id IS NOT NULL;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_online_created ON orders (created_at) WHERE channel = 'ONLINE';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_paid_at ON payments (paid_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_order_id ON payments (order_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_osh_order_id ON order_status_history (order_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_osh_action_created ON order_status_history (action_type, created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_shifts_closed_at ON shifts (closed_at);

-- Phải trả 0 dòng.
SELECT indexrelid::regclass FROM pg_index WHERE NOT indisvalid;
