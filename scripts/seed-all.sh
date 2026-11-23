#!/usr/bin/env bash
# Seed toàn bộ dữ liệu dev. CHỈ dùng cho DB dev. Cần BE đã chạy ít nhất 1 lần (để có bảng).
# Chạy:  bash backend/scripts/seed-all.sh
# Chạy lại được. Lưu ý phần Ca làm việc xóa mọi ca hiện có rồi tạo lại.
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
CONTAINER="${CONTAINER:-laundry-postgres}"
DB="${DB:-laundry}"

# Ảnh mẫu cho máy, đồ khách, sự cố: BE đọc file từ backend/uploads/
mkdir -p "$DIR/../uploads"
cp "$DIR"/seed-assets/*.png "$DIR/../uploads/"

docker exec -i "$CONTAINER" psql -U laundry -d "$DB" -q -v ON_ERROR_STOP=1 < "$DIR/seed-dev-full.sql"

echo "Xong. Mật khẩu chung: Seed013-fcf0d0f7 (seed_admin, seed_staff, seed_staff2, khách 0999990011)"
