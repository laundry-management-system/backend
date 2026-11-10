# Kiểm thử API bằng Postman

Bộ kiểm thử cho backend Laundry, gồm khoảng 155 request có sẵn assert.

| File | Nội dung |
|---|---|
| `laundry.postman_collection.json` | Collection: các request chia theo thư mục, mỗi request có test |
| `local.postman_environment.json` | Environment cho máy local (`http://localhost:8080`) |
| `build-collection.mjs` | Script sinh 2 file trên (xem mục 7) |

## 1. Chuẩn bị

```bash
docker compose up -d          # PostgreSQL + Redis
./mvnw spring-boot:run        # BE chạy ở http://localhost:8080
```

Redis bắt buộc phải chạy (đăng nhập dùng Redis để đếm số lần sai).

## 2. Import vào Postman

1. Postman → **Import** → chọn cả 2 file trong thư mục `postman/`.
2. Góc trên bên phải, chọn environment **Laundry - Local**.
3. Mở environment, điền **Current value** cho `adminPassword` (mật khẩu tài khoản `admin`, hỏi người phụ trách module auth).

> ⚠️ Chỉ điền vào cột **Current value**. Cột này chỉ lưu trên máy bạn. **Không** điền mật khẩu vào file JSON rồi commit lên git.

## 3. Chạy

- **Chạy cả bộ:** chuột phải collection → **Run collection** → **Run**.
- **Chạy từng request:** luôn chạy `00 - Đăng nhập Admin` trước để có token.

Các thư mục phụ thuộc nhau theo thứ tự (03 lấy id dịch vụ, 04 tạo khách, 05–06 dùng khách đó để tạo đơn...). Khi chạy lẻ một thư mục, hãy chạy các thư mục trước nó ít nhất một lần.

| Thư mục | Nội dung |
|---|---|
| 00 | Health, đăng nhập Admin, sai mật khẩu, thiếu token |
| 01 | Tạo và đăng nhập tài khoản STAFF (tùy chọn, xem mục 4) |
| 02 | Cài đặt |
| 03 | Dịch vụ & bảng giá |
| 04 | Khách hàng |
| 05 | Luồng đơn chuẩn: tiếp nhận → phân loại → xử lý → hoàn thành → nhắn khách → thanh toán → giao → tra cứu công khai |
| 06 | Thao tác Admin: dời hẹn, sửa giá, lùi trạng thái, hủy thanh toán, ghi chênh lệch, hủy đơn |
| 07 | Đồ tồn, nhật ký hệ thống |
| 08 | Phân quyền: NV gọi API của Admin phải bị 403 (cần tài khoản STAFF) |
| 09 | Voucher: tạo, kiểm tra mã, áp vào đơn, hết lượt, hủy đơn trả lượt |
| 10 | Máy giặt & máy sấy: thêm, bảo trì, ngừng sử dụng |
| 11 | Phiếu sự cố (giặt lại, giảm giá, bồi thường, từ chối) và Thanh toán–giao một lần (checkout) |
| 12 | Báo cáo doanh thu, báo cáo đơn hàng & dịch vụ, dashboard |
| 13 | Hồi quy các lỗi đã sửa theo `plan.md` (tên test ghi giai đoạn, ví dụ `[GĐ3]`) |

Mỗi request đều có test chung **"Không lỗi 500"**. Khi API trả lỗi 4xx, nội dung lỗi (`detail`, `errors`) được in ra **Console** của Postman để dễ dò.

**Dữ liệu sinh ra:** mỗi lần chạy sẽ tạo 1 khách hàng mới (SĐT ngẫu nhiên đầu `08`), 1 dịch vụ thử (đã tắt), 1 voucher (đã tắt), 1 máy (đã ngừng sử dụng), khoảng 6 đơn hàng và 3 phiếu sự cố. Chỉ nên chạy trên DB dev.

**Lưu ý:** test "Bồi thường vượt mức" dựa vào tham số `COMPENSATION_TERMS` là một con số (mặc định `100000`). Nếu đổi tham số này thành văn bản thì BE không áp mức trần và test đó sẽ FAIL.

## 4. Tài khoản STAFF (để chạy thư mục 08)

Nhân viên mới phải đặt mật khẩu qua link gửi email. Token của link nằm trong Redis, nên có thể lấy ra trực tiếp mà không cần email:

1. Chạy request **01 → Tạo nhân viên mới**. Postman tự lưu `staffUserId` và `staffUsername` vào environment.
2. Lấy token đặt mật khẩu từ Redis (thay `<id>` bằng `staffUserId`):
   ```bash
   docker exec laundry-redis redis-cli GET laundry:auth:setup_password:user:<id>
   ```
3. Dán kết quả vào biến `staffSetupToken` của environment.
4. Chạy **01 → Đặt mật khẩu nhân viên** (dùng `staffPassword`, mặc định `Staff@123456`), rồi **01 → Đăng nhập nhân viên**.

Chỉ cần làm một lần. Những lần sau, request tạo nhân viên tự bỏ qua vì `staffUsername` đã có. Nếu chưa có `staffToken`, các request dùng tài khoản STAFF sẽ tự động bị bỏ qua (skip), không bị tính là FAIL.

## 5. Chạy bằng dòng lệnh (Newman)

```bash
npx newman run postman/laundry.postman_collection.json \
  -e postman/local.postman_environment.json \
  --env-var adminPassword=<mật khẩu admin>
```

## 6. Cập nhật collection

Collection được sinh từ `build-collection.mjs`. Thêm hoặc sửa request trong script rồi chạy (từ thư mục `backend`):

```bash
node postman/build-collection.mjs postman
```

Cũng có thể sửa trực tiếp trong Postman rồi **Export** (Collection v2.1) đè lên file JSON, nhưng khi đó script sẽ không còn khớp với file.

Ngoài ra, danh sách đầy đủ mọi endpoint luôn có ở Swagger UI: `http://localhost:8080/swagger-ui.html`. Có thể import link `http://localhost:8080/v3/api-docs` vào Postman để có collection tự sinh (không kèm test).
