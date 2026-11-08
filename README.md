# Đồ án: Xây dựng ứng dụng RESTful API cho dịch vụ mạng (Đề 11)

> **Môn học**: Dịch vụ mạng / Lập trình mạng  
> **Kiến trúc**: RESTful API + WebSocket STOMP (Mở rộng)  
> **Ngôn ngữ & Framework**: Java 17, Spring Boot 3, Spring Security (Stateless JWT), Spring Data JPA, H2 / MySQL  

---

## 📖 Giới thiệu đề tài

Hệ thống cung cấp dịch vụ mạng hoàn chỉnh cho ứng dụng **Chat nhóm thời gian thực** kết hợp các dịch vụ quản lý dữ liệu cá nhân, quản trị người dùng và giám sát hệ thống mạng thông qua RESTful API và giao thức WebSocket STOMP.

### Các tính năng cốt lõi:
1. **Xác thực & Bảo mật (Authentication & Authorization)**:
   - Đăng ký, đăng nhập stateless cấp mã thông báo JWT (JJWT `0.12.6`).
   - Mật khẩu mã hóa bảo mật chuẩn BCrypt.
   - Phân quyền theo vai trò (`ROLE_USER`, `ROLE_ADMIN`).
2. **Quản lý dữ liệu người dùng (CRUD Data Service)**:
   - Thêm, sửa, xóa, xem dữ liệu cá nhân (chỉ chủ sở hữu dữ liệu mới có quyền thao tác).
3. **Dịch vụ Chat nhóm (RESTful & WebSocket STOMP)**:
   - Tạo phòng chat, danh sách phòng, tham gia phòng (`/api/rooms`).
   - Gửi tin nhắn và xem lịch sử tin nhắn của phòng qua REST API.
   - **Mở rộng WebSocket STOMP**: Gửi nhận tin nhắn tức thời (`/topic/room.{roomId}`), xác thực JWT ở bắt tay kết nối STOMP và kiểm soát quyền vào phòng. Kênh báo lỗi cá nhân riêng qua `/user/queue/errors`.
4. **Quản trị hệ thống (Admin Portal)**:
   - Xem danh sách người dùng, thay đổi phân quyền (nâng lên `ADMIN` hoặc hạ xuống `USER`), xóa tài khoản người dùng.
5. **Ghi nhật ký giám sát mạng (Audit & Request Logging)**:
   - `RequestLoggingFilter` tự động ghi nhận mọi request HTTP (Method, URL, IP máy khách, Username, HTTP Status, thời gian xử lý) vào cơ sở dữ liệu `request_logs`.
6. **Giao diện trực quan tích hợp**:
   - `chat.html`: Giao diện chat trực quan với STOMP.js thời gian thực.
   - `admin.html`: Bảng điều khiển quản trị viên và bảng xem nhật ký mạng.

---

## 🏛️ Kiến trúc hệ thống

Dự án áp dụng mô hình phân lớp rõ ràng:
- **Controller Layer** (`com.example.network.controller`): Tiếp nhận request, bọc toàn bộ phản hồi bằng `ResponseEntity<T>` với HTTP Status Code tương ứng, ghi log chi tiết với `@Slf4j`.
- **Service Layer** (`com.example.network.service` & `service.impl`): Tách biệt Interface và Implementation (`AuthServiceImpl`, `DataServiceImpl`, `RoomServiceImpl`, `MessageServiceImpl`, `AdminServiceImpl`, `LogServiceImpl`). Chứa 100% logic nghiệp vụ bằng Java thuần (`for`, `if`), không dùng lambda/stream trong nghiệp vụ.
- **Repository Layer** (`com.example.network.repository`): Kế thừa `JpaRepository` của Spring Data JPA.
- **Entity & DTO Layer** (`com.example.network.entity`):
  - Entity: `User`, `DataItem`, `ChatRoom`, `RoomMember`, `ChatMessage`, `RequestLog`.
  - Request DTOs (`entity.request`): `RegisterRequest`, `LoginRequest`, `DataRequest`, `RoomRequest`, `ChatMessageRequest`, `RoleRequest`.
  - Response DTOs (`entity.response`): `AuthResponse`, `UserResponse`, `UserSummary`, `ChatMessageResponse`, `ApiMessageResponse`.
- **Security & Logging Layer** (`com.example.network.security`, `config`, `logging`): `JwtAuthFilter`, `JwtUtil`, `WebSocketAuthInterceptor`, `RequestLoggingFilter`, `SecurityConfig`, `DataInitializer`.

---

## 🚀 Hướng dẫn cài đặt & Khởi chạy

### 1. Yêu cầu môi trường
- **Java**: JDK 17 trở lên.
- **Gradle**: Sử dụng sẵn `gradlew` đi kèm dự án.
- **Hệ quản trị CSDL**: H2 Database (tích hợp sẵn dạng file) hoặc MySQL 8.x.

### 2. Cấu hình cơ sở dữ liệu

Tệp cấu hình tại `src/main/resources/application.properties`:

#### Cách A: Chạy với H2 Database (Mặc định - Chạy ngay không cần cài đặt CSDL)
```properties
spring.datasource.url=jdbc:h2:file:./data/networkdb
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
```

#### Cách B: Chạy với MySQL
Tạo database trước trong MySQL:
```sql
CREATE DATABASE networkdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Cấu hình trong `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/networkdb?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=mật_khẩu_mysql_của_bạn
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### 3. Khởi chạy ứng dụng
Mở terminal tại thư mục gốc của dự án và chạy:

- Trên **Windows**:
  ```cmd
  gradlew.bat bootRun
  ```
  hoặc
  ```powershell
  ./gradlew bootRun
  ```
- Trên **Linux / macOS**:
  ```bash
  chmod +x gradlew
  ./gradlew bootRun
  ```

---

## 🔑 Tài khoản mặc định hệ thống

Hệ thống có cơ chế `DataInitializer` tự động khởi tạo sẵn tài khoản quản trị khi ứng dụng khởi chạy lần đầu:
- **Tài khoản**: `admin`
- **Mật khẩu**: `admin123`
- **Quyền**: `ROLE_ADMIN`

---

## 🌐 Giao diện Web Client

Sau khi khởi chạy ứng dụng thành công tại cổng `8080`:
- **Giao diện Chat nhóm thời gian thực**: [http://localhost:8080/chat.html](http://localhost:8080/chat.html)
- **Giao diện Quản trị & Nhật ký request**: [http://localhost:8080/admin.html](http://localhost:8080/admin.html)

---

## 📡 Danh mục RESTful API

| Method | Endpoint | Quyền truy cập | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Đăng ký tài khoản người dùng mới |
| `POST` | `/api/auth/login` | Public | Đăng nhập và nhận mã Bearer JWT Token |
| `GET` | `/api/data/hello` | Authenticated | Test xác thực token |
| `GET` | `/api/data` | Authenticated | Lấy danh sách dữ liệu của người dùng hiện tại |
| `GET` | `/api/data/{id}` | Authenticated | Xem chi tiết 1 bản ghi dữ liệu (chỉ chủ sở hữu) |
| `POST` | `/api/data` | Authenticated | Thêm bản ghi dữ liệu mới |
| `PUT` | `/api/data/{id}` | Authenticated | Cập nhật bản ghi dữ liệu (chỉ chủ sở hữu) |
| `DELETE`| `/api/data/{id}` | Authenticated | Xóa bản ghi dữ liệu (chỉ chủ sở hữu) |
| `GET` | `/api/rooms` | Authenticated | Lấy danh sách tất cả các phòng chat |
| `GET` | `/api/rooms/mine` | Authenticated | Lấy danh sách các phòng mà người dùng đã tham gia |
| `POST` | `/api/rooms` | Authenticated | Tạo phòng chat mới (người tạo tự động vào phòng) |
| `POST` | `/api/rooms/{id}/join` | Authenticated | Tham gia vào một phòng chat |
| `GET` | `/api/rooms/{id}/members` | Authenticated | Danh sách thành viên trong phòng (chỉ thành viên) |
| `GET` | `/api/rooms/{id}/messages` | Authenticated | Lấy lịch sử tin nhắn phòng có phân trang |
| `POST` | `/api/rooms/{id}/messages` | Authenticated | Gửi tin nhắn vào phòng chat qua REST API |
| `GET` | `/api/profile/me` | Authenticated | Lấy hồ sơ cá nhân của người dùng hiện tại |
| `PUT` | `/api/profile/me` | Authenticated | Cập nhật hồ sơ (họ tên, bio, avatar) của chính mình |
| `GET` | `/api/profile/{username}` | Authenticated | Xem hồ sơ công khai của một người dùng bất kỳ |
| `POST` | `/api/friends/request` | Authenticated | Gửi lời mời kết bạn tới người dùng khác |
| `GET` | `/api/friends` | Authenticated | Lấy danh sách bạn bè hiện tại (ACCEPTED) |
| `GET` | `/api/friends/requests/pending` | Authenticated | Lấy danh sách lời mời kết bạn đang chờ mình duyệt |
| `POST` | `/api/friends/requests/{id}/accept` | Authenticated | Chấp nhận lời mời kết bạn |
| `POST` | `/api/friends/requests/{id}/decline` | Authenticated | Từ chối lời mời kết bạn |
| `DELETE`| `/api/friends/{friendUsername}` | Authenticated | Hủy kết bạn |
| `POST` | `/api/direct-messages` | Authenticated | Gửi tin nhắn riêng 1-1 cho bạn bè |
| `GET` | `/api/direct-messages/{friendUsername}` | Authenticated | Lấy lịch sử tin nhắn 1-1 có phân trang |
| `GET` | `/api/direct-messages/conversations` | Authenticated | Danh sách các cuộc trò chuyện 1-1 gần nhất |
| `PUT` | `/api/direct-messages/{id}/read` | Authenticated | Đánh dấu tin nhắn đã đọc |
| `GET` | `/api/admin/users` | `ROLE_ADMIN` | Xem danh sách toàn bộ tài khoản người dùng |
| `PUT` | `/api/admin/users/{id}/role` | `ROLE_ADMIN` | Thay đổi quyền tài khoản (`USER` <-> `ADMIN`) |
| `DELETE`| `/api/admin/users/{id}` | `ROLE_ADMIN` | Xóa tài khoản người dùng khỏi hệ thống |
| `GET` | `/api/admin/error-logs` | `ROLE_ADMIN` | Xem nhật ký lỗi ngoại lệ (có phân trang, lọc theo status, userId, thời gian) |
| `GET` | `/api/logs` | `ROLE_ADMIN` | Xem danh sách nhật ký các request HTTP gần nhất |
| `GET` | `/api/logs/exceptions` | `ROLE_ADMIN` | Truy vấn danh sách lỗi ngoại lệ đã lưu trong database |

---

## ⚡ WebSocket STOMP Endpoint

- **Endpoint kết nối STOMP**: `/ws` (Xác thực JWT token qua Header `Authorization: Bearer <token>` tại frame `CONNECT`).
- **Gửi tin nhắn phòng chat**: `/app/chat.send/{roomId}` với payload `{"content": "..."}`.
- **Kênh nhận tin nhắn phòng**: Đăng ký nhận tại `/topic/room.{roomId}` (Yêu cầu phải là thành viên của phòng).
- **Kênh nhận tin nhắn riêng 1-1**: Đăng ký nhận tại `/topic/direct.{username}` (Nhận tin nhắn thời gian thực khi bạn bè gửi tới).
- **Kênh nhận thông báo lỗi riêng**: `/user/queue/errors`.
