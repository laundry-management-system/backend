-- 029: dọn bảng yêu cầu đặt đơn cũ và backfill orders.channel. Chạy một lần SAU khi triển khai BE 029 (quickstart bước 5).
--
-- TRƯỚC KHI CHẠY: sao lưu DB, đây là bản sao duy nhất của yêu cầu đặt đơn cũ:
--   pg_dump -Fc -f before-029.dump "$DB_URL"
-- Khôi phục nếu cần: pg_restore -d "$DB_URL" -t booking_requests -t booking_request_items before-029.dump
--
-- Chạy:   psql "$DB_URL" -v ON_ERROR_STOP=1 -f backend/scripts/cleanup-029-booking-requests.sql
-- Dev:    docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/cleanup-029-booking-requests.sql
-- Chạy lại bao nhiêu lần cũng được.
--
-- FK duy nhất trỏ vào các bảng này là booking_request_items -> booking_requests, nên xóa items trước.
-- Không dùng CASCADE: nếu có view/FK khác phụ thuộc, script dừng và rollback thay vì xóa ngầm.

\set ON_ERROR_STOP on

BEGIN;

DROP TABLE IF EXISTS booking_request_items;
DROP TABLE IF EXISTS booking_requests;

-- Chỉ chạm các dòng chưa có kênh (SC-005); Postgres đã điền default 'OFFLINE' khi thêm cột, đây là phòng hờ.
UPDATE orders SET channel = 'OFFLINE' WHERE channel IS NULL;

COMMIT;
