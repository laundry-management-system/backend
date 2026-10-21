# Laundry Management System - Backend

Dự án Backend cho Hệ thống Quản lý Giặt là (Môn học SE114).

---

## 🚀 Khởi chạy dự án (Getting Started)

### 1. Yêu cầu môi trường
- **Java**: 21 trở lên (Khuyến nghị Eclipse Temurin hoặc OpenJDK 21)
- **Docker & Docker Compose** (để chạy cơ sở dữ liệu PostgreSQL)
- **Maven** (hoặc dùng wrapper `./mvnw` có sẵn trong thư mục)

---

### 2. Chạy Cơ Sở Dữ Liệu PostgreSQL
Khởi chạy container PostgreSQL 16 bằng Docker Compose:
```bash
docker compose up -d
```
> Cơ sở dữ liệu sẽ chạy ở cổng `5432`, thông tin đăng nhập mặc định:
> - **Database**: `laundry`
> - **Username**: `laundry`
> - **Password**: `laundry`

---

### 3. Khởi chạy Ứng dụng Spring Boot
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / MacOS
./mvnw spring-boot:run
```

Sau khi khởi chạy thành công:
- **API Base URL**: `http://localhost:8080/api`
- **Kiểm tra Health**: `http://localhost:8080/api/health`
- **Swagger UI & OpenAPI Docs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### 4. Chạy Unit Test
```bash
.\mvnw.cmd test
```

---

## 📖 Quy định Cấu Trúc & Kiến Trúc Dự Án
Toàn bộ nhóm bắt buộc đọc và tuân thủ tài liệu kiến trúc tại:  
👉 **[ARCHITECTURE.md](./ARCHITECTURE.md)** (Quy định chi tiết về mô hình Modular Monolith 3-layer, 3 quy tắc cốt lõi, Naming Conventions và Checklist 5 bước thêm module mới).

