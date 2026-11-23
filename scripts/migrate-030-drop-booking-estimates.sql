-- 030: bỏ các cột tạm tính của đơn đặt online, đổi tên phí ship (giữ dữ liệu).
-- Chạy TRƯỚC KHI khởi động BE 030: dừng BE cũ -> chạy script -> khởi động BE mới (quickstart bước 3).
--
-- TRƯỚC KHI CHẠY: sao lưu DB:
--   pg_dump -Fc -f before-030.dump "$DB_URL"
-- Khôi phục nếu cần: pg_restore --clean -d "$DB_URL" -t order_fulfillments before-030.dump
--
-- Chạy:   psql "$DB_URL" -v ON_ERROR_STOP=1 -f backend/scripts/migrate-030-drop-booking-estimates.sql
-- Dev:    docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/migrate-030-drop-booking-estimates.sql
-- Chạy lại bao nhiêu lần cũng được; DB mới (chưa có bảng/cột) thì không làm gì.
--
-- Quay lại BE cũ không mất dữ liệu (thay vì restore dump), chạy rồi mới khởi động BE cũ:
--   BEGIN;
--   ALTER TABLE order_fulfillments RENAME COLUMN pickup_fee TO estimated_pickup_fee;
--   ALTER TABLE order_fulfillments RENAME COLUMN delivery_fee TO estimated_delivery_fee;
--   ALTER TABLE order_fulfillments ADD COLUMN estimated_items jsonb NOT NULL DEFAULT '[]';
--   ALTER TABLE order_fulfillments ADD COLUMN estimated_subtotal numeric(12,0) NOT NULL DEFAULT 0;
--   ALTER TABLE order_fulfillments ADD COLUMN estimated_express_fee numeric(12,0) NOT NULL DEFAULT 0;
--   ALTER TABLE order_fulfillments ADD COLUMN estimated_total numeric(12,0) NOT NULL DEFAULT 0;
--   ALTER TABLE order_fulfillments ADD COLUMN service_area_id bigint;
--   ALTER TABLE order_fulfillments ADD COLUMN service_area_name varchar(100);
--   UPDATE order_fulfillments SET service_area_id = COALESCE(pickup_area_id, delivery_area_id),
--                                 service_area_name = CASE WHEN pickup_area_id IS NOT NULL THEN pickup_area_name ELSE delivery_area_name END;
--   ALTER TABLE order_fulfillments DROP COLUMN pickup_area_id, DROP COLUMN pickup_area_name,
--                                  DROP COLUMN delivery_area_id, DROP COLUMN delivery_area_name;
--   COMMIT;

\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    pair text[];
    has_old boolean;
    has_new boolean;
BEGIN
    FOREACH pair SLICE 1 IN ARRAY ARRAY[
        ['estimated_pickup_fee', 'pickup_fee'],
        ['estimated_delivery_fee', 'delivery_fee']
    ] LOOP
        SELECT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = current_schema() AND table_name = 'order_fulfillments' AND column_name = pair[1])
          INTO has_old;
        SELECT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = current_schema() AND table_name = 'order_fulfillments' AND column_name = pair[2])
          INTO has_new;

        IF has_old AND NOT has_new THEN
            EXECUTE format('ALTER TABLE order_fulfillments RENAME COLUMN %I TO %I', pair[1], pair[2]);
        ELSIF has_old AND has_new THEN
            -- BE mới đã khởi động trước và Hibernate tạo cột mới: chép giá trị cũ sang rồi bỏ cột cũ.
            EXECUTE format('UPDATE order_fulfillments SET %2$I = COALESCE(%1$I, %2$I)', pair[1], pair[2]);
            EXECUTE format('ALTER TABLE order_fulfillments DROP COLUMN %I', pair[1]);
        END IF;
    END LOOP;
END $$;

ALTER TABLE IF EXISTS order_fulfillments DROP COLUMN IF EXISTS estimated_items;
ALTER TABLE IF EXISTS order_fulfillments DROP COLUMN IF EXISTS estimated_subtotal;
ALTER TABLE IF EXISTS order_fulfillments DROP COLUMN IF EXISTS estimated_express_fee;
ALTER TABLE IF EXISTS order_fulfillments DROP COLUMN IF EXISTS estimated_total;

-- 030 addendum: khu vực tách theo chiều lấy / giao (FR-029, FR-032).
ALTER TABLE IF EXISTS order_fulfillments
    ADD COLUMN IF NOT EXISTS pickup_area_id bigint,
    ADD COLUMN IF NOT EXISTS pickup_area_name varchar(100),
    ADD COLUMN IF NOT EXISTS delivery_area_id bigint,
    ADD COLUMN IF NOT EXISTS delivery_area_name varchar(100);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'order_fulfillments' AND column_name = 'service_area_id') THEN
        -- EXECUTE: cột cũ có thể không tồn tại lúc biên dịch khối.
        EXECUTE 'UPDATE order_fulfillments SET pickup_area_id = service_area_id, pickup_area_name = service_area_name
                 WHERE dropoff_method = ''PICKUP'' AND pickup_area_id IS NULL';
        EXECUTE 'UPDATE order_fulfillments SET delivery_area_id = service_area_id, delivery_area_name = service_area_name
                 WHERE return_method = ''DELIVERY'' AND delivery_area_id IS NULL';
    END IF;
END $$;

ALTER TABLE IF EXISTS order_fulfillments DROP COLUMN IF EXISTS service_area_id;
ALTER TABLE IF EXISTS order_fulfillments DROP COLUMN IF EXISTS service_area_name;

COMMIT;
