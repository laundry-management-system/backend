-- 031 hoàn tác: đưa DB về dạng BE cũ đọc được, không mất dữ liệu, không đổi tiền.
-- Chạy khi BE 031 đã dừng, TRƯỚC KHI khởi động BE cũ (quickstart bước 6).
--
-- Chạy:   psql "$DB_URL" -v ON_ERROR_STOP=1 -f backend/scripts/rollback-031-wash-modes.sql
-- Dev:    docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/rollback-031-wash-modes.sql
--
-- Giữ nguyên bảng wash_modes, service_wash_modes và các cột wash_mode_* / requested_service_prices /
-- legacy_service_surcharges trên orders (BE cũ bỏ qua), nên chạy lại migrate-031 sau đó vẫn an toàn.
-- Cột wash_modes.code (và uk_wash_modes_code) cũng giữ nguyên; BE cũ không đọc bảng wash_modes.
-- Giới hạn: giá sửa ở BE cũ sau khi hoàn tác KHÔNG tự đồng bộ khi migrate lại (migrate chỉ tạo liên kết giá cho
-- dịch vụ chưa có liên kết nào); sửa lại giá theo hình thức ở BE mới nếu cần.
-- express_surcharge = giá Giặt nhanh - giá Tiêu chuẩn có thể âm: BE cũ vẫn tính đúng tiền, chỉ báo lỗi
-- "Phụ phí giặt nhanh không được âm" khi admin lưu lại chính dịch vụ đó; không cắt về 0 vì sẽ đổi tiền.
-- Giá của hình thức thứ ba trở đi không có cột cũ tương ứng nhưng vẫn nằm trong service_wash_modes.

\set ON_ERROR_STOP on

BEGIN;

-- 1. Hai setting giờ trả, lấy từ giờ của hình thức NORMAL / EXPRESS.
INSERT INTO settings (setting_key, setting_value, description, updated_at)
SELECT v.setting_key, m.completion_hours::text, v.description, now()
  FROM (VALUES
        ('NORMAL',  'BOOKING_RETURN_GAP_HOURS_NORMAL',  'Giờ nhận lại tối thiểu sau giờ gửi (giặt thường)'),
        ('EXPRESS', 'BOOKING_RETURN_GAP_HOURS_EXPRESS', 'Giờ nhận lại tối thiểu sau giờ gửi (giặt nhanh)')
       ) v(order_type, setting_key, description)
  JOIN wash_modes m ON m.legacy_order_type = v.order_type
ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = now();

-- 2. Cột giá cũ của services từ liên kết NORMAL / EXPRESS; dịch vụ không có liên kết nào giữ nguyên.
UPDATE services s
   SET current_price     = p.normal_price,
       standard_hours    = COALESCE(s.standard_hours, p.normal_hours),
       express_supported = p.express_price IS NOT NULL,
       express_surcharge = p.express_price - p.normal_price,
       express_hours     = CASE WHEN p.express_price IS NOT NULL THEN COALESCE(s.express_hours, p.express_hours)
                                ELSE s.express_hours END
  FROM (SELECT l.service_id,
               COALESCE(max(l.price) FILTER (WHERE m.legacy_order_type = 'NORMAL'), min(l.price)) AS normal_price,
               max(l.price) FILTER (WHERE m.legacy_order_type = 'EXPRESS') AS express_price,
               (SELECT completion_hours FROM wash_modes WHERE legacy_order_type = 'NORMAL')  AS normal_hours,
               (SELECT completion_hours FROM wash_modes WHERE legacy_order_type = 'EXPRESS') AS express_hours
          FROM service_wash_modes l
          JOIN wash_modes m ON m.id = l.wash_mode_id
         GROUP BY l.service_id) p
 WHERE p.service_id = s.id;

ALTER TABLE services
    ALTER COLUMN current_price SET NOT NULL,
    ALTER COLUMN standard_hours SET NOT NULL,
    ALTER COLUMN express_supported SET NOT NULL;

-- Bỏ trước khi điền order_type (BE cũ dùng uk_orders_intake_request_type); migrate lại sẽ tạo lại.
DROP INDEX IF EXISTS uk_orders_intake_request_new;
DROP INDEX IF EXISTS uk_wash_modes_single_default;

-- 3. order_type của đơn tạo ở BE mới: theo hình thức (hình thức mới -> NORMAL). Đơn mới có express_surcharge = 0
--    nên BE cũ tính expressFee = 0, tổng tiền không đổi.
UPDATE orders o
   SET order_type = COALESCE(m.legacy_order_type, 'NORMAL')
  FROM orders o2
  LEFT JOIN wash_modes m ON m.id = o2.wash_mode_id
 WHERE o2.id = o.id AND o.order_type IS NULL;

ALTER TABLE orders ALTER COLUMN order_type SET NOT NULL;

COMMIT;
