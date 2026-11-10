# Kế hoạch chỉnh sửa Backend theo BE.md

> Dựa trên tài liệu `BE.md` (rà soát màn mockup), code BE tại `main` (`9dcf13e`) và code FE tại `67d7553` (chỉ đọc để đối chiếu).
> Cập nhật: 2026-11-09
>
> **Phạm vi: chỉ sửa BE.** Những chỗ FE cần đổi theo được liệt kê ở mục 6 để nhóm FE tự làm.
>
> Các đường dẫn BE bên dưới tính từ `src/main/java/com/laundry/modules/`.
> Cỡ việc: **S** ≈ vài giờ · **M** ≈ 1–2 ngày · **L** ≈ 3–5 ngày.

---

## 1. Đối chiếu BE.md với code hiện tại

### 1.1. Mục trong BE.md cần chỉnh lại

| Mục | BE.md ghi | Code thực tế | Kết luận |
|---|---|---|---|
| WF-A02 khóa tạm | "Kiểm tra có thật sự khóa không" | `AuthService` đếm số lần sai trong Redis, khóa theo `MAX_LOGIN_FAIL` / `LOCK_MINUTES`, ghi audit `ACCOUNT_LOCKED` | **Đã chạy đúng → chuyển sang ✅.** Chỉ còn cửa sổ đếm 5 phút đang ghi cứng (`auth/application/AuthService.java:115`) |
| WF-E04 resolution | "Kiểm tra chỉ Admin và có audit" | Có `@PreAuthorize ADMIN`, có ghi audit `PAYMENT_VOID` / `PAYMENT_REFUND` | Đúng yêu cầu, nhưng audit được ghi bằng SQL thuần qua `JpaAuditLogAdapter`, truyền action dạng chuỗi → nên chuyển sang `AuditLogService` |
| WF-H08 reset mật khẩu | "Kiểm tra `resend-setup-mail` đủ chưa" | `UserService.resend()` gọi `user.requirePending()` → chỉ dùng được cho tài khoản **chưa kích hoạt** | **Chưa đủ → cần API reset mật khẩu riêng** |

### 1.2. Vấn đề phát hiện thêm (BE.md chưa nêu)

| # | Vấn đề | Vị trí | Xử lý ở |
|---|---|---|---|
| A | NV tự gửi số tiền giảm giá: `recalcTotals(request.discountAmount())` nhận giá trị từ client | `order/application/OrderService.java:365` | GĐ 1 |
| B | Module order **không ghi audit log nào**, dù đã có sẵn action `STATUS_ROLLBACK`, `DUE_DATE_CHANGE`, `ORDER_PRICE_EDIT` | `order/application/OrderService.java` | GĐ 3 |
| C | Thanh toán và giao đồ cập nhật `orders` bằng SQL thuần, nên **không ghi `order_status_history`**. Xác nhận thanh toán không ghi audit `PAYMENT_CONFIRM` | `payments/infrastructure/JpaOrderIntegrationAdapter.java`, `payments/application/PaymentService.java:109` | GĐ 3 |
| D | `adjustPrice` ghi đè `totalAmount`, bỏ qua `recalcTotals` → mâu thuẫn với voucher | `order/domain/Order.java:311` | GĐ 1 + mục 5 |
| E | Role `'EMPLOYEE'` không tồn tại (enum `Role` chỉ có `ADMIN`, `STAFF`) | `payments/presentation/PaymentController.java:27`, `OrderEngagementController.java:32` | GĐ 6 |
| F | Tiền tố mã đơn `"LD"` ghi cứng | `order/application/CodeSequenceService.java:24` | GĐ 0 |
| G | Tên tiệm trên phiếu ghi cứng `"Tiệm Giặt Ủi Thông Minh SE114"` | `order/application/dto/OrderReceiptResponse.java:76` | GĐ 6 |
| H | Action `CUSTOMER_PHONE_CHANGE` đã khai báo nhưng đổi SĐT khách chưa ghi audit | `customer/application/CustomerService.java` | GĐ 6 |
| I | Repo **chưa có thư mục `src/test`** | — | Mục 4 |
| J | `GET /orders?phone=` lọc bằng `LIKE '%phone%'`. FE dùng nó để xem lịch sử đơn của **một** khách → SĐT `0901` sẽ khớp cả đơn của khách khác | `order/domain/OrderRepository.java` | GĐ 6 |
| K | `GET /api/orders/track?phone=` là **public**, không giới hạn tần suất, trả toàn bộ đơn theo SĐT. FE không dùng (tra cứu công khai đã có `/api/public/orders/lookup` có rate limit) | `order/presentation/OrderController.java`, `common/config/SecurityConfig.java:33` | GĐ 3 |
| L | Đặt mật khẩu lần đầu không kiểm độ dài (chỉ `@NotBlank`), FE đang phải tự chặn 8–255 ký tự | `auth/application/dto/SetupPasswordRequest.java` | GĐ 6 |
| M | Các endpoint FE **không dùng**: `POST /orders/quick-create`, `POST /orders/estimate-return-time`, `POST /orders/{id}/deliver` (luồng cũ, giá ghi cứng, không gắn khách, giao không cần thanh toán) | `order/presentation/OrderController.java` | GĐ 3 |
| N | JSON sai cú pháp, giá trị enum không hợp lệ, gọi sai đường dẫn hoặc sai method đều rơi vào `@ExceptionHandler(Exception.class)` → trả **500** thay vì 400 / 404 / 405 | `common/exception/GlobalExceptionHandler.java` | GĐ 6 |
| O | Tra cứu công khai kiểm mã đơn bằng regex ghi cứng `^LD-...` → sẽ hỏng khi đổi `ORDER_CODE_PREFIX` ở GĐ 0 | `payments/application/dto/PublicOrderLookupRequest.java` | GĐ 0 |

### 1.3. Những gì FE đang chờ BE (từ ghi chú `shortcut:` / `handoff` trong code FE)

| Ghi chú trong FE | FE đang tạm làm | Xử lý ở |
|---|---|---|
| `voucherMock.ts` (handoff #8) | Voucher giả 4 mã, kiểm tra ở client | GĐ 1 |
| `constants.ts` (handoff #6), `useShopInfo.ts` | Giờ mở cửa ghi cứng; lấy tên tiệm bằng cách gọi API **in phiếu** của một đơn | GĐ 0 |
| `useCheckout.ts` (handoff 011 #1) | Gọi 2 lệnh: thu tiền rồi giao đồ | GĐ 3 |
| `AdminActionsCard.tsx` (handoff 008 #9) | Chỉ ẩn nút ở FE, BE chưa chặn vai trò | GĐ 3 |
| `types.ts:229` (handoff 010 #1) | Hiện id người xác nhận thanh toán thay vì tên | GĐ 3 |
| `OrderIncidentCard.tsx` (handoff 008 #5) | Chỗ giữ, chưa có dữ liệu | GĐ 4 |
| `ordersApi.ts:117` (handoff 008 #2) | Đếm theo trạng thái bằng N lần gọi `/orders?size=1` | GĐ 6 |
| `rules.ts:12` (research R2) | Đoán "chưa phân loại" bằng cách đếm dòng đồ | GĐ 6 |
| `customersApi.ts:31` (BE-2), `CustomersPage.tsx:41` (BE-1) | Lọc `status` không có tác dụng; cột thống kê hiện "—" | GĐ 6 |
| `services/data/dto.ts:34` (BE-S1) | **Đã gửi `priceChangeReason`** nhưng BE bỏ qua | GĐ 6 |
| `session.ts` (BE-6) | Mở nhiều tab cùng refresh token có thể bị đăng xuất một tab | Ngoài phạm vi (xem mục 5) |
| `draftStorage.ts` (research R8) | Nháp phân loại chỉ lưu trên trình duyệt | Ngoài phạm vi |

---

## 2. Các giai đoạn

### Giai đoạn 0: Nền tảng dùng chung, cỡ S–M

Làm trước vì gỡ chặn cho FE và là nền cho mọi module sau.

**0.1. Settings (WF-H09)**: `setting/`
- [ ] Bỏ `@PreAuthorize("hasRole('ADMIN')")` ở cấp class (`SettingController.java:24`), chỉ đặt cho `PATCH /{settingKey}`. `GET /api/settings` cho mọi user đã đăng nhập.
- [ ] Thêm `PATCH /api/settings` (ADMIN) nhận `[{settingKey, value}]`: validate toàn bộ trước, lưu trong một transaction, mỗi key thay đổi ghi một audit `SETTING_UPDATED`. Giữ nguyên `PATCH /{settingKey}` vì FE đang dùng.
- [ ] `SettingKey` thêm `STORE_PHONE` (STRING), `ORDER_CODE_PREFIX` (STRING). Seed trong `data.sql` (`'0900000000'`, `'LD'`).
- [ ] `CodeSequenceService.getNextOrderCode()` đọc prefix từ `ORDER_CODE_PREFIX`.
- [ ] `PublicOrderLookupRequest`: nới regex mã đơn thành `^[A-Z0-9]{1,5}-[0-9]{8}-[0-9]{3}$` để không phụ thuộc tiền tố `LD` (vấn đề O).

**0.2. Audit log (WF-I03)**: `log/`
- [ ] `EntityType`: thêm `MACHINE`, `VOUCHER`, `INCIDENT`.
- [ ] `AuditAction`: thêm `VOUCHER_MANAGE`, `MACHINE_MANAGE`, `INCIDENT_CREATE`, `ORDER_CANCEL(true)`, `SERVICE_STATUS`, `CUSTOMER_VIP`, `CUSTOMER_STATUS`, `PASSWORD_RESET`. (`INCIDENT_APPROVE`, `MACHINE_STATUS`, `PRICE_CHANGE`, `PAYMENT_CONFIRM` đã có.)
- [ ] `AuditLog` thêm cột `entity_code` (varchar 50, nullable). `AuditEntry` thêm `entityCode` (giữ constructor cũ để không phải sửa các chỗ gọi hiện có).
- [ ] `SettingService.log()` ghi `entityCode = settingKey.name()` (bỏ cách dùng `ordinal()` làm id).
- [ ] `AuditLogSummaryResponse` thêm `reason`, `role` (lấy từ `User`), `entityCode`. Chỉ **thêm** field, không đổi tên field cũ, để FE hiện tại không vỡ.

**0.3. Lịch sử đơn**: `order/domain/HistoryActionType.java`
- [ ] Thêm `PAYMENT_CONFIRMED`, `PAYMENT_VOIDED`, `DELIVERED`, `INCIDENT_REOPEN`.

### Giai đoạn 1: Voucher (UC19 · BM10 · QĐ12 · WF-H05, WF-C03), cỡ L

**Domain**: module mới `voucher/`. **Đặt tên field khớp kiểu `Voucher` FE đã có** (`frontend/src/features/orders/domain/types.ts`, `voucher.ts`):

| Field | Kiểu | Ghi chú |
|---|---|---|
| `code` | String, unique | lưu viết hoa, trim |
| `discountType` | `PERCENT` \| `AMOUNT` | |
| `discountValue` | BigDecimal | % thì 1–100 |
| `startDate`, `endDate` | `LocalDate` (`YYYY-MM-DD`) | **tính cả hai đầu**, so theo ngày giờ Việt Nam |
| `minOrderValue` | BigDecimal | so với **tiền hàng** (subtotal) |
| `maxUses`, `usedCount` | int | |
| `status` | `ACTIVE` \| `DISABLED` | FE dùng `DISABLED`, không phải `INACTIVE` |
| `createdBy` | Long | |

**API**

| Method | Path | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/api/vouchers?search&status` | ADMIN | phân trang |
| GET | `/api/vouchers/{id}` | ADMIN | |
| POST | `/api/vouchers` | ADMIN | audit `VOUCHER_MANAGE` |
| PUT | `/api/vouchers/{id}` | ADMIN | không sửa `code`/`discountType` khi `usedCount > 0` |
| PATCH | `/api/vouchers/{id}/status` | ADMIN | bật / tắt, không xóa |
| GET | `/api/vouchers/check?code=&subtotal=` | STAFF | Trả `{ok: true, discount, voucher}` hoặc `{ok: false, reason}`. `reason` dùng đúng 6 mã FE đã có: `NOT_FOUND`, `DISABLED`, `NOT_STARTED`, `EXPIRED`, `USED_UP`, `BELOW_MIN` |

Thứ tự kiểm tra giống `checkVoucher()` của FE: tồn tại → bị tắt → chưa đến ngày → hết hạn → hết lượt → dưới tối thiểu. Số tiền giảm: `PERCENT` làm tròn `subtotal × value / 100`; cả hai loại đều ≤ subtotal.

**Tích hợp với đơn**
- [ ] `UpdateOrderItemsRequest` thêm `voucherCode` (nullable).
- [ ] `OrderService.classifyIntakeOrder`: **bỏ qua `discountAmount` client gửi** (vấn đề A). BE kiểm voucher như trên, tự tính tiền giảm, lưu `order.voucherId` và `discountAmount`. Không có `voucherCode` thì giảm = 0.
- [ ] Giữ lượt bằng câu atomic `UPDATE vouchers SET used_count = used_count + 1 WHERE id = :id AND used_count < max_uses`; nếu `updated == 0` thì báo hết lượt. Khi phân loại lại mà đổi mã hoặc bỏ mã thì trả lượt của mã cũ; giữ nguyên mã thì không trừ thêm.
- [ ] Thêm `VoucherService.release(voucherId)` cho luồng hủy đơn và sửa giá (GĐ 3).
- [ ] `OrderDetailResponse`, `OrderReceiptResponse` thêm `voucherCode`.

> ⚠️ **Thay đổi phá vỡ tương thích:** sau GĐ 1, FE hiện tại (đang gửi `discountAmount` từ voucher giả) sẽ thấy giảm giá bằng 0 cho đến khi FE chuyển sang gửi `voucherCode`. Cần báo nhóm FE trước khi merge (mục 6).

### Giai đoạn 2: Quản lý máy (UC14, UC18 · BM8 · QĐ10 · WF-H04), cỡ M

FE chưa có màn này, nên BE tự định nghĩa hợp đồng API.

**Domain**: module mới `machine/`
- `Machine`: `machineCode` (MG01 cho máy giặt, MS01 cho máy sấy, sinh tự động), `name`, `type` (WASHER/DRYER), `capacityKg`, `status` (READY/MAINTENANCE/RETIRED), `note`.

| Method | Path | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/api/machines?type&status` | STAFF | |
| GET | `/api/machines/{id}` | STAFF | |
| POST | `/api/machines` | ADMIN | audit `MACHINE_MANAGE` |
| PUT | `/api/machines/{id}` | ADMIN | audit `MACHINE_MANAGE` |
| PATCH | `/api/machines/{id}/retire` | ADMIN | ngừng sử dụng; **không có DELETE** |
| PATCH | `/api/machines/{id}/status` | STAFF | chỉ cho READY ⇄ MAINTENANCE; bắt buộc lý do khi chuyển sang bảo trì; máy RETIRED không đổi được nữa; audit `MACHINE_STATUS` |

*Ngoài phạm vi BE.md:* lượt chạy máy (BR-E03→E09) trong tài liệu đề tài. Xem mục 5.

### Giai đoạn 3: Luồng đơn, thanh toán, giao đồ (WF-D04, D05, E01→E03), cỡ M–L

Làm ngay sau Voucher vì hủy đơn phải trả lượt voucher.

**3.1. Phân quyền và audit cho thao tác Admin (WF-D04)**: `order/`
- [ ] `@PreAuthorize("hasRole('ADMIN')")` cho `POST /{id}/revert-status` (`OrderController.java:182`) và `PATCH /{id}/price` (`:204`).
- [ ] Ghi audit cho các thao tác sửa đơn (vấn đề B): `revertStatus` → `STATUS_ROLLBACK`, `reschedule` → `DUE_DATE_CHANGE`, `adjustPrice` → `ORDER_PRICE_EDIT`, `cancel` → `ORDER_CANCEL`.

**3.2. Hủy đơn (WF-D05, BR-D05)**
- [ ] `Order.cancel()` (`Order.java:326`): chỉ cho hủy khi `status == RECEIVED`.
- [ ] `OrderService.cancel`: nếu đơn đã thanh toán thì gọi `PaymentService` để chuyển giao dịch sang REFUNDED (audit `PAYMENT_REFUND`, lý do = lý do hủy); nếu có voucher thì gọi `VoucherService.release`. Tất cả trong một transaction.

**3.3. Thanh toán và giao đồ (WF-E01→E03)**: `payments/`
- [ ] Thêm `POST /api/orders/{orderCode}/checkout`: một transaction gồm lock đơn → kiểm tra `COMPLETED` → confirm payment → deliver. Request: `{amount, method, referenceCode, receiverName, receiverRelation}`. Response: `DeliveryResponse`. Giữ nguyên `/payments` và `/delivery` vì FE đang dùng.
- [ ] `OrderIntegration` thêm `appendHistory(orderId, actionType, oldValue, newValue, reason, performedBy)`, INSERT vào `order_status_history`. Gọi khi confirm, resolve (void/refund) và deliver (vấn đề C).
- [ ] `PaymentService.confirm` ghi audit `PAYMENT_CONFIRM`.
- [ ] Chuyển `PaymentService.resolve` sang `AuditLogService.log()`, xóa `AuditLogPort` + `JpaAuditLogAdapter`.
- [ ] `PaymentResponse` **thêm** `confirmedByName`, `voidedByName` (lấy theo lô từ `UserRepository.findAllById`). Giữ `confirmedBy` (id) vì FE đang đọc.

**3.4. Dọn endpoint không dùng (vấn đề K, M)**: đã đối chiếu FE, không màn nào gọi các endpoint này.
- [ ] Xóa `POST /orders/quick-create`, `POST /orders/estimate-return-time`, `POST /orders/{id}/deliver` cùng các method service và DTO đi kèm (`QuickCreateOrderRequest`, `EstimateReturnTime*`, `DeliverOrderRequest`).
- [ ] Xóa `GET /orders/track` và bỏ khỏi `PUBLIC_PATHS` (`SecurityConfig.java:33-34`).
- [ ] Kiểm tra enum `ServiceType` còn được dùng ở đâu; nếu chỉ phục vụ luồng cũ thì giữ field trên entity (đơn cũ trong DB) nhưng bỏ logic tính giá.

### Giai đoạn 4: Phiếu sự cố (UC15, UC24 · BM9 · QĐ11 · WF-H06, H07), cỡ L

Phụ thuộc: GĐ 2 (`machineId`), GĐ 0 (audit, history). FE mới có chỗ giữ (`OrderIncidentCard.tsx`), BE tự định nghĩa hợp đồng API.

**Domain**: module mới `incident/`
- `Incident`: `incidentCode` (SC-yyyyMMdd-NNN, dùng `CodeSequenceService`), `orderId`, `orderItemId?`, `type` (LOST/TORN/FADED/SHRUNK/OTHER), `description`, `machineId?`, `staffInvolved?`, `documentIds` (ảnh hiện tại, dùng module `document`), `resolution` (REWASH/DISCOUNT/COMPENSATE), `redoStage` (WASH/DRY/BOTH/NONE), `compensationAmount`, `status` (PENDING/APPROVED/REJECTED), `createdBy`, `approvedBy`, `approvedAt`, `decisionNote`.
- `Order` thêm `processingCount` (mặc định 1).

| Method | Path | Quyền |
|---|---|---|
| POST | `/api/incidents` | STAFF (audit `INCIDENT_CREATE`) |
| GET | `/api/incidents?status&type&from&to` | STAFF |
| GET | `/api/incidents/{id}` | STAFF (kèm ảnh lúc nhận đồ của dòng đồ để đối chiếu) |
| GET | `/api/orders/{id}/incidents` | STAFF (thẻ Sự cố ở WF-D02) |
| POST | `/api/incidents/{id}/approve` | ADMIN |
| POST | `/api/incidents/{id}/reject` | ADMIN (bắt buộc lý do) |

Khi duyệt:
- [ ] Chỉ duyệt phiếu `PENDING`; kiểm tra `compensationAmount` ≤ `COMPENSATION_TERMS`.
- [ ] Nếu phương án là REWASH: đưa đơn về `PROCESSING`, tăng `processingCount`, ghi history `INCIDENT_REOPEN` kèm mã phiếu (BR-D17).
- [ ] Ghi audit `INCIDENT_APPROVE` (cả duyệt lẫn từ chối).

### Giai đoạn 5: Báo cáo và Dashboard (UC26, UC27 · BM11, BM12 · QĐ13, QĐ14 · WF-I01, I02, B01), cỡ M

Module mới `report/`, chỉ đọc, dùng truy vấn tổng hợp, mọi ngày giờ quy về **Asia/Ho_Chi_Minh**.

| Method | Path | Quyền | Nội dung |
|---|---|---|---|
| GET | `/api/reports/revenue?from&to&groupBy=DAY\|MONTH` | ADMIN | Từ `payments.status = CONFIRMED`, nhóm theo `paid_at`, join `orders.status <> 'CANCELLED'`, chia theo `method` (CASH / BANK_TRANSFER) |
| GET | `/api/reports/orders?from&to` | ADMIN | Số đơn và giá trị trung bình theo `orderType`; số đơn trễ (`completedAt > dueAt`, hoặc chưa xong mà quá hạn); doanh thu theo dịch vụ (cộng `order_items.line_total` theo `service_id`); tổng `expressFee`, `whitesFee`, `discountAmount` |
| GET | `/api/dashboard/summary` | ADMIN | Số đơn nhận hôm nay, tiền thu hôm nay, số đơn trễ, số đồ tồn (dùng lại `OverdueDaysProvider`) |

- [ ] Thêm index `payments(paid_at)`, `orders(received_at)`.

### Giai đoạn 6: Bổ sung field, bộ lọc, dọn dẹp, mỗi mục cỡ S

| Màn | Thay đổi | File chính |
|---|---|---|
| WF-F01 | `GET /customers` nhận `status`. `CustomerResponse` thêm `orderCount`, `totalSpent`, `lastOrderAt`, tính bằng một query aggregate theo lô `customerId` của trang (tránh N+1) | `customer/domain/CustomerRepository.java:26`, `CustomerService`, `CustomerResponse` |
| WF-F02 | `PATCH /customers/{id}/vip` (ADMIN, audit `CUSTOMER_VIP`). `PATCH /{id}/status` thêm ADMIN + chặn INACTIVE khi còn đơn chưa `DELIVERED`/`CANCELLED` (BR-B03), audit `CUSTOMER_STATUS`. Đổi SĐT ghi audit `CUSTOMER_PHONE_CHANGE` (vấn đề H). Lịch sử đơn theo khách: xem dòng WF-D01 (FE đang dùng `/orders`, không cần endpoint riêng) | `customer/presentation/CustomerController.java:85` |
| WF-D01 / B02 | `searchOrders` chuyển sang `Specification` (theo mẫu `log/domain/AuditLogSpecs`), thêm `customerId`, `receivedFrom`, `receivedTo`, `paymentStatus`, `orderType`, `classified` (= đã có dòng đồ). `phone` đổi sang **so khớp chính xác** (vấn đề J); tìm gần đúng theo SĐT vẫn dùng `search`. Thêm `GET /orders/counts` → `{RECEIVED: n, PROCESSING: n, ...}`. `OrderResponse` thêm `classified` | `order/domain/OrderRepository.java`, `OrderController`, `OrderResponse` |
| WF-D02 | `OrderDetailResponse` thêm `lastNotifiedAt`, `notifyCount` (đã có trên entity), `paymentMethod`, `paidAt` (từ payment CONFIRMED), `voucherCode` | `order/application/dto/OrderDetailResponse.java` |
| WF-K02 | `OrderReceiptResponse`: `shopName`, `hotline` lấy từ `STORE_NAME`, `STORE_PHONE` (vấn đề G) | `order/application/dto/OrderReceiptResponse.java:76` |
| WF-H03 | `UpdateServiceRequest` thêm **`priceChangeReason`** (đúng tên FE đang gửi). `updateService` ghi audit `PRICE_CHANGE` (old/new của giá, phụ phí nhanh, tối thiểu) khi các trường này đổi; đổi trạng thái ghi `SERVICE_STATUS`. Thêm `GET /services/{id}/price-history` (đọc audit theo `SERVICE` + id) | `service/application/LaundryServiceService.java:76` |
| WF-H08 | `POST /api/users/{id}/reset-password` (ADMIN): đưa tài khoản về trạng thái chờ đặt lại, gửi mail bằng `PasswordSetupService` sẵn có, audit `PASSWORD_RESET` | `auth/application/UserService.java` |
| Đặt mật khẩu | `SetupPasswordRequest.password` thêm `@Size(min = 8, max = 255)` (vấn đề L) | `auth/application/dto/SetupPasswordRequest.java` |
| Xử lý lỗi | Bắt `HttpMessageNotReadableException` và `MethodArgumentTypeMismatchException` → 400, `NoResourceFoundException` → 404, `HttpRequestMethodNotSupportedException` → 405 (vấn đề N) | `common/exception/GlobalExceptionHandler.java` |
| Dọn | Bỏ role `'EMPLOYEE'` (vấn đề E). Đưa cửa sổ đếm đăng nhập sai 5 phút vào cấu hình | `payments/presentation/*`, `AuthService.java:115` |

---

## 3. Thứ tự thực hiện

```
GĐ 0 ─┬─> GĐ 1 Voucher ──> GĐ 3 Luồng đơn/thanh toán ─┐
      ├─> GĐ 2 Máy ───────> GĐ 4 Sự cố ────────────────┼─> GĐ 5 Báo cáo
      └─> GĐ 6 (các mục độc lập, làm xen kẽ bất cứ lúc nào)
```

1. **GĐ 0.** Settings đang chặn các màn khác. Audit/history là nền cho mọi module sau.
2. **GĐ 1 Voucher → GĐ 3**, vì hủy đơn cần trả lượt voucher, checkout cần history.
3. **GĐ 2 Máy** song song với bước 2.
4. **GĐ 4 Sự cố** sau khi có Máy.
5. **GĐ 5 Báo cáo** khi số liệu thanh toán, voucher, phụ phí đã ổn định.
6. **GĐ 6**: các mục độc lập, chia cho nhiều người.

Mỗi giai đoạn nên là một nhánh + PR riêng để review dễ. Nguyên tắc chung: **chỉ thêm field vào response, không đổi tên hay xóa field FE đang đọc**, trừ các chỗ ghi rõ là thay đổi phá vỡ tương thích.

## 4. Kiểm thử

Repo hiện **chưa có `src/test`**. Đề xuất:

- [ ] Dựng khung test tích hợp (`@SpringBootTest` + Testcontainers PostgreSQL/Redis, hoặc DB test riêng) ở GĐ 0.
- [ ] Mỗi giai đoạn tối thiểu có test cho:
  - Voucher: đủ 6 mã lỗi, làm tròn %, giảm không vượt subtotal, hai request áp cùng lúc lượt cuối
  - Hủy đơn: ngoài trạng thái `RECEIVED` bị chặn; đơn đã thanh toán → hoàn tiền + trả lượt voucher
  - Checkout: đơn chưa hoàn thành, số tiền sai, đơn đã thanh toán
  - Phân quyền: NV gọi API Admin → 403
  - Báo cáo: đơn hủy và giao dịch đã void/refund không được tính

## 5. Cần nhóm chốt trước khi code

- [ ] **Voucher và sửa giá (vấn đề D):** khi Admin `adjustPrice`, giữ voucher hay bỏ voucher và trả lượt? *Đề xuất: bỏ voucher, trả lượt.*
- [ ] **Thời điểm tính lượt voucher:** giữ lượt ngay khi phân loại hay chỉ trừ khi thanh toán? *Đề xuất: ngay khi phân loại.*
- [ ] **Phương án DISCOUNT của sự cố:** tự trừ vào tổng tiền đơn chưa thanh toán, hay chỉ ghi nhận số tiền? *Đề xuất: tự trừ (cộng vào `discountAmount`), chỉ cho chọn khi đơn chưa thanh toán.*
- [ ] **Lượt chạy máy (BR-E03→E09):** có nằm trong phạm vi đợt này không? *Đề xuất: chưa làm, FE chưa có màn.*
- [x] ~~**Endpoint cũ `POST /orders/quick-create`**~~ → **Đã chốt: xóa.** FE không gọi (đã kiểm tra). Xem GĐ 3.4.
- [ ] **Refresh token nhiều tab (BE-6):** có cần cho phép token cũ dùng lại trong vài giây sau khi xoay vòng không? *Đề xuất: để sau, FE đã xử lý tạm trong một tab.*

## 6. Việc FE cần làm theo (BE không sửa FE)

Gửi danh sách này cho nhóm FE khi các giai đoạn tương ứng được merge.

| Sau GĐ | FE cần đổi |
|---|---|
| 0 | `constants.ts`: đọc `OPENING_TIME`/`CLOSING_TIME` từ `GET /settings`. `useShopInfo.ts`: lấy tên tiệm, địa chỉ, hotline từ `GET /settings` thay vì gọi API in phiếu. Trang Cài đặt thêm `STORE_PHONE`, `ORDER_CODE_PREFIX`; có thể lưu nhiều tham số một lần bằng `PATCH /settings`. Nhật ký hiển thị thêm `reason`, `role`, `entityCode` |
| 1 | ⚠️ **Bắt buộc:** `ordersApi.updateItems` gửi `voucherCode` thay cho `discountAmount`. `voucherCheck.ts` gọi `GET /vouchers/check`, xóa `voucherMock.ts`. Thêm màn quản lý voucher (WF-H05) |
| 2 | Thêm màn máy giặt/sấy (WF-H04) |
| 3 | `useCheckout.ts` dùng `POST /orders/{code}/checkout`. Hiện `confirmedByName`. `CancelOrderModal.tsx`: bỏ dòng "Hệ thống chưa tự trả lượt voucher" vì BE đã tự hoàn tiền và trả lượt |
| 4 | `OrderIncidentCard.tsx` gọi `GET /orders/{id}/incidents`; thêm màn danh sách và lập/duyệt phiếu (WF-H06, H07) |
| 5 | Màn báo cáo (WF-I01, I02); dashboard (WF-B01) gọi `/dashboard/summary` |
| 6 | `ordersApi.ts`: dùng `GET /orders/counts`. `rules.ts`: dùng cờ `classified`. `customersApi.ts`: lịch sử đơn lọc theo `customerId`; bỏ ghi chú lọc `status`. `CustomersPage.tsx`: hiện thống kê. `ServiceFormModal.tsx`: bỏ ghi chú "BE chưa lưu lịch sử giá". `rules.ts` (auth): có thể bỏ chặn độ dài mật khẩu ở FE |
