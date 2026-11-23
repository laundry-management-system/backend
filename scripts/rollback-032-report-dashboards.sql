-- Rollback 032: bỏ 9 index và cột order_status_history.reason_code.
-- Chạy: psql "$DB_URL" -v ON_ERROR_STOP=1 -f backend/scripts/rollback-032-report-dashboards.sql (không dùng -1).

DROP INDEX CONCURRENTLY IF EXISTS idx_orders_received_at;
DROP INDEX CONCURRENTLY IF EXISTS idx_orders_delivered_at;
DROP INDEX CONCURRENTLY IF EXISTS idx_orders_customer_received;
DROP INDEX CONCURRENTLY IF EXISTS idx_orders_online_created;
DROP INDEX CONCURRENTLY IF EXISTS idx_payments_paid_at;
DROP INDEX CONCURRENTLY IF EXISTS idx_payments_order_id;
DROP INDEX CONCURRENTLY IF EXISTS idx_osh_order_id;
DROP INDEX CONCURRENTLY IF EXISTS idx_osh_action_created;
DROP INDEX CONCURRENTLY IF EXISTS idx_shifts_closed_at;

SET lock_timeout = '5s';
ALTER TABLE order_status_history DROP COLUMN IF EXISTS reason_code;
