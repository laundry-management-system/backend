-- Dữ liệu test cho Khu vực giao nhận (Cài đặt → Khu vực giao nhận, trang công khai). CHỈ dùng cho DB dev.
--
-- Chạy:   docker exec -i laundry-postgres psql -U laundry -d laundry -v ON_ERROR_STOP=1 < backend/scripts/seed-dev-service-areas.sql
-- Chạy lại bao nhiêu lần cũng được: trùng tên (không phân biệt hoa thường) thì cập nhật phí và trạng thái về như dưới.
-- Không xoá khu vực nào (nghiệp vụ không có xoá), khu vực ngoài danh sách giữ nguyên.
--
-- Kết quả: 12 khu vực, 10 đang phục vụ (có 1 miễn phí cả hai lượt, 1 phí lấy khác phí giao), 2 ngừng phục vụ.

BEGIN;

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

COMMIT;
