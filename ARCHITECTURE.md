# Kiến Trúc Backend & Quy Định Cấu Trúc Thư Mục

> **Tài liệu chuẩn hóa kiến trúc Backend (Spring Boot 3-Layer Modular Monolith)**  
> **Dự án**: Laundry Management System (Hệ thống Quản lý Giặt là) - SE114  
> **Package gốc**: `com.laundry`

---

## 1. Tổng Quan Kiến Trúc Backend

Backend được thiết kế theo mô hình **Modular Monolith** kết hợp **Clean Architecture rút gọn (3-Layer)** cho từng module nghiệp vụ. Hệ thống cung cấp RESTful API theo hướng **Stateless**, xác thực bằng **JWT Bearer Token** và đồng nhất toàn bộ định dạng lỗi theo chuẩn quốc tế **RFC 9457 (`ProblemDetail`)**.

```text
HTTP Request (Client / Postman / Swagger)
   │
   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ JwtFilter (Xác thực Bearer Token, nạp Authentication vào SecurityContext)│
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 1. Presentation Layer (Controller)                                     │
│    - Tiếp nhận request, validate cú pháp (@Valid)                      │
│    - Phân quyền theo vai trò (@PreAuthorize)                          │
│    - Ủy quyền toàn bộ xử lý cho Application Service                    │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ (Chỉ truyền / nhận DTO)
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. Application Layer (Service)                                         │
│    - Điều phối quy trình nghiệp vụ, quản lý Transaction (@Transactional)│
│    - Chuyển đổi DTO ◄► Entity (qua ResponseDTO.from(entity))           │
│    - Gọi Domain Entities và JpaRepositories                           │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. Domain Layer (Entity, Enum, Repository)                             │
│    - Entity JPA (kế thừa BaseEntity), chứa logic trạng thái nội tại    │
│    - Enum trạng thái nghiệp vụ                                         │
│    - JpaRepository giao tiếp với PostgreSQL Database                   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
                             PostgreSQL 16
```

---

## 2. Tech Stack Chi Tiết

| Phân loại | Công nghệ / Thư viện | Phiên bản | Vai trò & Mục đích sử dụng |
| :--- | :--- | :--- | :--- |
| **Ngôn ngữ** | Java | 21 (LTS) | Sử dụng record cho DTO, pattern matching, switch expressions. |
| **Framework** | Spring Boot | 4.x | Khung phát triển ứng dụng chính (Spring Web, Spring Security, Spring Data JPA). |
| **Cơ sở dữ liệu** | PostgreSQL | 16 | Hệ quản trị CSDL quan hệ chính (chạy container qua Docker Compose). |
| **ORM / Data** | Hibernate / Spring Data JPA | Kèm Spring Boot | Quản lý thực thể, tự động cập nhật schema (`ddl-auto: update`), CRUD repository. |
| **Xác thực** | Spring Security & JJWT | 0.13.0 | Stateless Session, giải mã và xác thực token JWT, BCrypt băm mật khẩu. |
| **Xử lý lỗi** | `@RestControllerAdvice` | RFC 9457 | Trả về định dạng `ProblemDetail` chuẩn hoá cho mọi HTTP error (400, 401, 403, 404, 500). |
| **Tài liệu API** | springdoc-openapi | 3.1.x | Tự động sinh tài liệu Swagger UI tại `/swagger-ui.html`, có sẵn nút gắn Bearer JWT. |
| **Tiện ích** | Lombok | Mới nhất | Giảm thiểu mã lặp (`@Getter`, `@RequiredArgsConstructor`, `@Slf4j`). |
| **Môi trường** | Docker Compose | Compose v2 | Cấu hình sẵn dịch vụ PostgreSQL 16 tại cổng 5432 để dev đồng bộ. |

---

## 3. Ba Quy Tắc Bắt Buộc (Core Principles)

> [!IMPORTANT]
> Toàn bộ thành viên khi phát triển backend phải tuân thủ nghiêm ngặt 3 quy tắc sau:
> 
> 1. **Controller KHÔNG ĐƯỢC gọi trực tiếp Repository**: Mọi truy vấn và tương tác với dữ liệu bắt buộc phải đi qua Service.
> 2. **Service KHÔNG ĐƯỢC trả Entity ra Controller**: Service chỉ tiếp nhận Request DTO và trả về Response DTO. Entity tuyệt đối không để lộ ra tầng Controller hay client.
> 3. **Logic nghiệp vụ KHÔNG ĐƯỢC nằm trong Controller**: Controller chỉ làm nhiệm vụ tiếp nhận HTTP request, validate `@Valid` và gọi Service. Nghiệp vụ tính toán hoặc thay đổi trạng thái phải nằm trong Service hoặc phương thức của Entity.

---

## 4. Chi Tiết Các Tầng (Layers Specification)

### 4.1. Tầng dùng chung (`common/`)
Nơi chứa toàn bộ hạ tầng kỹ thuật xuyên suốt hệ thống (Cross-cutting concerns). Không chứa logic nghiệp vụ của bất kỳ tính năng cụ thể nào.

- **`common/config/`**:
  - `SecurityConfig.java`: Cấu hình `SecurityFilterChain`, `SessionCreationPolicy.STATELESS`, vô hiệu hóa CSRF, mở các public routes (`/api/health`, `/api/auth/**`, Swagger UI), cấu hình `AuthenticationEntryPoint` và `AccessDeniedHandler` trả về `ProblemDetail`.
  - `CorsConfig.java`: Cho phép gọi API trực tiếp từ frontend local (`http://localhost:5173`).
  - `OpenApiConfig.java`: Định nghĩa cấu hình OpenAPI 3 và tích hợp JWT Bearer Authentication scheme.
- **`common/entity/`**:
  - `BaseEntity.java`: `@MappedSuperclass` định nghĩa sẵn:
    - `id`: Khóa chính `GenerationType.IDENTITY` (Long).
    - `createdAt`: `@CreationTimestamp`, không cho phép update.
    - `updatedAt`: `@UpdateTimestamp`.
- **`common/exception/`**:
  - `NotFoundException.java`: Kế thừa `RuntimeException`, biểu diễn lỗi không tìm thấy tài nguyên (trả về 404).
  - `BusinessException.java`: Biểu diễn vi phạm quy tắc nghiệp vụ (trả về 400).
  - `GlobalExceptionHandler.java`: `@RestControllerAdvice` bắt các ngoại lệ (`NotFoundException`, `BusinessException`, `MethodArgumentNotValidException`, `AuthenticationException`, `AccessDeniedException`, `Exception`) và đóng gói thành `ProblemDetail`.
- **`common/security/`**:
  - `JwtUtil.java`: Tạo chuỗi JWT từ username + roles, kiểm tra tính hợp lệ và trích xuất claims.
  - `JwtFilter.java`: `OncePerRequestFilter` trích xuất `Authorization: Bearer <token>`, giải mã và đưa `UsernamePasswordAuthenticationToken` vào `SecurityContextHolder`.
  - `CustomUserDetailsService.java`: Triển khai `UserDetailsService` phục vụ việc xác thực đăng nhập.

---

### 4.2. Tầng nghiệp vụ theo Module (`modules/<feature>/`)
Mỗi nghiệp vụ là một package độc lập gồm 3 layer:

#### 1. Domain Layer (`modules/<feature>/domain/`)
- **Entity**:
  - Kế thừa từ `BaseEntity`.
  - Được phép chứa các phương thức nghiệp vụ gắn chặt với dữ liệu của entity đó (Rich Domain Model).  
    *Ví dụ*:
    ```java
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new BusinessException("Không thể hủy đơn hàng đã hoàn thành");
        }
        this.status = OrderStatus.CANCELLED;
    }
    ```
- **Enum**: Định nghĩa các trạng thái nghiệp vụ (ví dụ: `OrderStatus`, `PaymentMethod`).
- **Repository**:
  - Interface kế thừa `JpaRepository<Entity, Long>`.
  - Chỉ chứa các câu truy vấn (Spring Data Derived Queries hoặc `@Query`).

#### 2. Application Layer (`modules/<feature>/application/`)
- **DTOs (`dto/`)**:
  - **Request DTO**: Class chứa dữ liệu đầu vào từ client, bắt buộc gắn validation annotations (ví dụ: `@NotBlank`, `@NotNull`, `@Min`, `@Size`).
  - **Response DTO**: Ưu tiên sử dụng Java `record`. Cung cấp static factory method `from(Entity entity)` để ánh xạ dữ liệu:
    ```java
    public record OrderResponse(Long id, String customerName, OrderStatus status, Instant createdAt) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getCustomerName(), order.getStatus(), order.getCreatedAt());
        }
    }
    ```
- **Service (`*Service.java`)**:
  - Được đánh dấu `@Service` và `@RequiredArgsConstructor`.
  - Thêm `@Transactional` trên các method thêm/sửa/xóa, và `@Transactional(readOnly = true)` trên các method truy vấn.
  - Xử lý các quy tắc nghiệp vụ liên module, kiểm tra điều kiện trước khi lưu dữ liệu.

#### 3. Presentation Layer (`modules/<feature>/presentation/`)
- **Controller (`*Controller.java`)**:
  - Được đánh dấu `@RestController`, `@RequestMapping("/api/<resource>")`, `@RequiredArgsConstructor`.
  - Thêm `@Tag(name = "...")` và `@Operation(summary = "...")` cho Swagger documentation.
  - Bắt buộc dùng `@Valid` trước các `@RequestBody`.
  - Phân quyền endpoint bằng `@PreAuthorize("hasRole('ADMIN')")` khi cần.

---

## 5. Cấu Trúc Cây Thư Mục Toàn Diện

```text
backend/
├── docker-compose.yml                      # Định nghĩa dịch vụ PostgreSQL 16
├── pom.xml                                 # Khai báo dependency Maven
├── mvnw / mvnw.cmd                         # Maven Wrapper script
├── ARCHITECTURE.md                         # [Tài liệu này] Quy chuẩn kiến trúc Backend
├── README.md                               # Hướng dẫn setup và chạy backend
└── src/
    ├── main/
    │   ├── java/com/laundry/
    │   │   ├── LaundryApplication.java     # Class chạy chính (@SpringBootApplication)
    │   │   │
    │   │   ├── common/                     # Hạ tầng dùng chung cho mọi module
    │   │   │   ├── config/
    │   │   │   │   ├── SecurityConfig.java # Cấu hình FilterChain, Stateless, Route phân quyền
    │   │   │   │   ├── CorsConfig.java     # Cấu hình CORS cho Frontend gọi API
    │   │   │   │   └── OpenApiConfig.java  # Cấu hình Swagger UI + JWT Authorize Button
    │   │   │   ├── entity/
    │   │   │   │   └── BaseEntity.java     # ID, createdAt, updatedAt
    │   │   │   ├── exception/
    │   │   │   │   ├── BusinessException.java
    │   │   │   │   ├── NotFoundException.java
    │   │   │   │   └── GlobalExceptionHandler.java # Chuẩn hoá lỗi theo RFC 9457
    │   │   │   └── security/
    │   │   │       ├── JwtUtil.java
    │   │   │       ├── JwtFilter.java
    │   │   │       └── CustomUserDetailsService.java
    │   │   │
    │   │   └── modules/                    # Các module tính năng (Modular Monolith)
    │   │       ├── health/                 # Module kiểm tra tình trạng hệ thống
    │   │       │   ├── domain/
    │   │       │   │   └── HealthStatus.java
    │   │       │   ├── application/
    │   │       │   │   ├── HealthService.java
    │   │       │   │   └── dto/
    │   │       │   │       └── HealthResponse.java
    │   │       │   └── presentation/
    │   │       │       └── HealthController.java
    │   │       │
    │   │       ├── auth/                   # [Module mẫu xác thực sắp làm]
    │   │       │   ├── domain/             # User, Role, UserRepository
    │   │       │   ├── application/        # AuthService, LoginRequest, RegisterRequest, AuthResponse
    │   │       │   └── presentation/       # AuthController (/api/auth/login, /api/auth/register)
    │   │       │
    │   │       └── order/                  # [Module mẫu đơn hàng sắp làm]
    │   │           ├── domain/
    │   │           │   ├── Order.java          # Entity JPA
    │   │           │   ├── OrderStatus.java    # Enum trạng thái
    │   │           │   └── OrderRepository.java# Kế thừa JpaRepository<Order, Long>
    │   │           ├── application/
    │   │           │   ├── OrderService.java   # Logic nghiệp vụ đơn hàng
    │   │           │   └── dto/
    │   │           │       ├── CreateOrderRequest.java # Validate input (@NotBlank, @Min)
    │   │           │       ├── UpdateOrderStatusRequest.java
    │   │           │       └── OrderResponse.java      # Java record + OrderResponse.from(order)
    │   │           └── presentation/
    │   │               └── OrderController.java    # REST API endpoints (@Valid, @PreAuthorize)
    │   │
    │   └── resources/
    │       ├── application.yml             # Cấu hình DB, JWT secret, Hibernate, Swagger
    │       └── data.sql                    # Dữ liệu seed mẫu (tài khoản admin, bảng giá khởi tạo)
    │
    └── test/
        └── java/com/laundry/
            ├── LaundryApplicationTests.java
            └── common/security/SecurityTests.java
```

---

## 6. Quy Chuẩn Đặt Tên & Định Dạng (Naming Conventions)

- **Packages**: Chữ thường toàn bộ, không có ký tự đặc biệt (`com.laundry.modules.order.presentation`).
- **Entity**: Danh từ số ít, PascalCase (`Order`, `OrderItem`, `Customer`).
- **Bảng Database**: Danh từ số nhiều hoặc số ít thống nhất, lower_snake_case (`orders`, `order_items`, `customers`).
- **Cột Database**: lower_snake_case (`created_at`, `total_amount`, `customer_id`).
- **Repository Interface**: `<Entity>Repository` (`OrderRepository`).
- **Service Class**: `<Entity>Service` (`OrderService`).
- **Controller Class**: `<Entity>Controller` (`OrderController`).
- **Request DTO**: `<Action><Entity>Request` (`CreateOrderRequest`, `UpdateOrderRequest`).
- **Response DTO**: `<Entity>Response` hoặc `<Entity>DetailResponse` (`OrderResponse`).
- **Method tên hàm**: camelCase, động từ thể hiện rõ ý nghĩa:
  - Lấy dữ liệu: `getOrderById(Long id)`, `listOrders(...)`.
  - Thay đổi dữ liệu: `createOrder(...)`, `cancelOrder(Long id)`.

---

## 7. Quy Trình 5 Bước Tạo Một Module Nghiệp Vụ Mới

Khi bạn cần thêm một tính năng mới (ví dụ: `Customer`, `ServiceCatalog`, `Order`):

1. **Bước 1 (Domain)**: Tạo folder `modules/<tên_module>/domain/`. Tạo Entity kế thừa `BaseEntity`, tạo Enum trạng thái (nếu có), và tạo Interface Repository kế thừa `JpaRepository`.
2. **Bước 2 (DTOs)**: Tạo folder `modules/<tên_module>/application/dto/`. Viết các Request DTO (thêm validation `@NotBlank`, `@Positive`,...) và Response DTO (dùng `record` kèm static factory method `from(...)`).
3. **Bước 3 (Service)**: Tạo class `<Feature>Service` trong `application/`. Khai báo `@Service`, inject Repository, xử lý logic, đặt `@Transactional` khi thao tác ghi.
4. **Bước 4 (Controller)**: Tạo class `<Feature>Controller` trong `presentation/`. Khai báo `@RestController`, `@RequestMapping("/api/<resource>")`, thêm `@Valid` cho RequestBody, gắn `@Operation` cho Swagger.
5. **Bước 5 (Kiểm thử API)**:
   - Khởi động ứng dụng bằng `mvnw spring-boot:run`.
   - Mở Swagger UI tại [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
   - Kiểm tra các endpoint: chạy thử trường hợp thành công (200/201), trường hợp thiếu dữ liệu validate (400 ProblemDetail), và trường hợp không có quyền (401/403).
