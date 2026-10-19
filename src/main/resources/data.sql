-- Dữ liệu mẫu (tài khoản admin, danh mục...) chạy sau khi Hibernate tạo bảng.
-- Một đơn chỉ được có một payment đang CONFIRMED; các dòng VOIDED/REFUNDED được giữ làm lịch sử.
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_confirmed_order
    ON payments (order_id)
    WHERE status = 'CONFIRMED';
SELECT 1;
