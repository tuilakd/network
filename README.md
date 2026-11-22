# Mini Messenger & ChatWave – RESTful API Dịch Vụ Mạng (Đề 11)

> **Môn học**: Dịch vụ mạng / Lập trình mạng  
> **Đề tài**: Xây dựng ứng dụng RESTful API cho dịch vụ mạng (Đề tài 11)  
> **Mô hình**: Client – Server (Kiến trúc phân tầng)  
> **Backend**: Java 17, Spring Boot, Spring Security (JWT), Spring Data JPA, MySQL 8.x, WebSocket STOMP  
> **Desktop Client**: Java Swing (ChatWave) với giao diện hiện đại (Modern Flat UI, Dark/Light Mode)  

---

## Mục lục

1. [Giới thiệu tổng quan](#giới-thiệu-tổng-quan)
2. [Cấu trúc dự án](#cấu-trúc-dự-án)
3. [Công nghệ sử dụng](#công-nghệ-sử-dụng)
4. [Tính năng hệ thống](#tính-năng-hệ-thống)
5. [Cài đặt và Khởi chạy](#cài-đặt-và-khởi-chạy)
   - [Yêu cầu môi trường](#1-yêu-cầu-môi-trường)
   - [Cấu hình cơ sở dữ liệu](#2-tạo-cơ-sở-dữ-liệu-mysql)
   - [Biến môi trường](#3-biến-môi-trường-khuyến-nghị)
   - [Khởi chạy Backend (Spring Boot)](#4-khởi-chạy-backend-spring-boot)
   - [Khởi chạy Desktop Client (Java Swing)](#5-khởi-chạy-desktop-client-chatwave-java-swing)
6. [Tài khoản mặc định](#tài-khoản-mặc-định)
7. [Cấu hình gửi email OTP](#cấu-hình-gửi-email-quên-mật-khẩu)
8. [Danh mục RESTful API](#danh-mục-restful-api)
9. [Giao thức WebSocket STOMP](#giao-thức-websocket-stomp)
10. [Cơ chế bảo mật và Giám sát](#cơ-chế-bảo-mật-và-giám-sát)

---

## Giới thiệu tổng quan

**Mini Messenger** là hệ thống trao đổi thông tin, nhắn tin tức thời theo mô hình Client – Server:
- **Server (`network-backend`)**: Xây dựng bằng **Spring Boot**, cung cấp hệ thống RESTful API chuẩn hóa cùng kênh truyền thời gian thực **WebSocket STOMP**. Đảm nhiệm toàn bộ logic xác thực JWT, quản lý quan hệ bạn bè, phòng chat, lưu trữ tệp đính kèm và ghi nhận log giám sát.
- **Client chính thức (`clientswing` - ChatWave)**: Ứng dụng Desktop xây dựng bằng **Java Swing** với phong cách thiết kế hiện đại (Flat UI bo góc, đổ bóng mềm, hiệu ứng mờ dần, hỗ trợ chuyển đổi Dark / Light theme). Tích hợp đầy đủ các chức năng nhắn tin 1-1, chat nhóm, gửi ảnh/file ghi âm, quản trị hệ thống và có chế độ Demo Offline.
- **Client tham khảo (Legacy Web)**: Giao diện web tĩnh đặt tại thư mục `static` phục vụ kiểm thử nhanh qua trình duyệt.

---

## Cấu trúc dự án

Dự án được phân chia thành 2 phần rõ rệt:

```text
network/
├── network-backend/                # Module Backend (Spring Boot REST API)
│   ├── build.gradle                # Cấu hình phụ thuộc Gradle của Backend
│   └── src/
│       ├── main/
│       │   ├── java/com/example/network/
│       │   │   ├── config/         # Cấu hình Security, CORS, WebSocket, Async
│       │   │   ├── controller/     # Các REST Controller & WebSocket STOMP Controller
│       │   │   ├── entity/         # JPA Entities, DTO Request, DTO Response
│       │   │   ├── exception/      # Bắt lỗi toàn cục (GlobalExceptionHandler, DomainCode)
│       │   │   ├── logging/        # Bộ lọc RequestLoggingFilter, Cron dọn dẹp log
│       │   │   ├── repository/     # Spring Data JPA Repositories
│       │   │   ├── security/       # JWT Auth Filter, JwtUtil, RateLimiter
│       │   │   ├── service/        # Interface & Implementation nghiệp vụ
│       │   │   └── util/           # Hằng số và hàm tiện ích
│       │   └── resources/
│       │       ├── application.properties # Cấu hình database, mail, jwt, upload
│       │       └── static/         # Web client tĩnh demo (legacy)
│       └── test/                   # Unit test & Integration test
│
├── clientswing/                    # Module Desktop Client (Java Swing - ChatWave)
│   └── clientswing/
│       ├── src/chatwave/
│       │   ├── Main.java           # Điểm khởi chạy ứng dụng Swing
│       │   ├── AppFrame.java       # Khung cửa sổ chính, quản lý chuyển màn hình
│       │   ├── core/               # Quản lý Session, Backend interface, RemoteBackend, Models
│       │   ├── screens/            # Màn hình: Đăng nhập/Đăng ký, Chat chính, Quản trị
│       │   └── ui/                 # Bộ thư viện UI tự dựng (Theme, Button, Table, Card, Dialog)
│       └── build.xml               # Cấu hình biên dịch NetBeans / Ant
│
├── gradlew, gradlew.bat            # Gradle Wrapper dùng chung
├── settings.gradle                 # Khai báo cấu trúc module gốc
└── README.md                       # Tài liệu hướng dẫn dự án
```

---

## Công nghệ sử dụng

### 1. Backend
- **Ngôn ngữ & Nền tảng**: Java 17, Gradle.
- **Framework**: Spring Boot 3.x / 4.x (Spring Web, Spring Security, Spring Data JPA, Spring Validation, Spring Mail, Spring WebSocket).
- **Xác thực & Mã hóa**: JJWT (Java JWT `0.12.6`), BCrypt Password Hashing.
- **Cơ sở dữ liệu**: MySQL 8.x (kèm hỗ trợ H2 in-memory).
- **Giao thức mạng**: HTTP/1.1 RESTful, WebSocket STOMP.
- **Tiện ích**: Lombok, Slf4j.

### 2. Desktop Client (ChatWave)
- **Công nghệ**: Java Swing thuần, Java 2D Graphics.
- **Giao diện**: Flat UI tùy biến cao cấp, hỗ trợ khử răng cưa Font (Font Anti-aliasing), tối ưu hiển thị màn hình HiDPI, bảng màu Dark / Light linh hoạt.
- **Giao tiếp**: `HttpURLConnection` gọi REST API đồng bộ và đa luồng bất đồng bộ qua `Async Worker`.

---

## Tính năng hệ thống

| Phân hệ | Tính năng chi tiết |
|---|---|
| **Xác thực & Bảo mật** | - Đăng ký tài khoản mới, kiểm tra trùng lặp email / username.<br>- Đăng nhập cấp mã truy cập JWT Bearer (thời hạn 24h).<br>- Chống tấn công dò mật khẩu (Brute-force) với `LoginRateLimiter` (sai 5 lần khóa 5 phút).<br>- Quên mật khẩu qua mã OTP 6 số gửi về email (hiệu lực 10 phút). |
| **Hồ sơ cá nhân** | - Xem và cập nhật thông tin cá nhân (họ tên, bio, ngày sinh, số điện thoại, avatar).<br>- Xem thông tin công khai của người dùng khác. |
| **Bạn bè** | - Tìm kiếm người dùng theo tên.<br>- Gửi lời mời kết bạn, chấp nhận hoặc từ chối.<br>- Quản lý danh sách bạn bè, hủy kết bạn. |
| **Tin nhắn riêng (1-1)** | - Trò chuyện trực tiếp 1-1 giữa 2 người bạn.<br>- Xem danh sách các cuộc trò chuyện gần nhất, trạng thái đã đọc/chưa đọc.<br>- Thu hồi tin nhắn đã gửi hoặc xóa tin nhắn ở phía tôi. |
| **Chat nhóm (Rooms)** | - Tạo phòng chat riêng tư, đặt tên phòng, thêm thành viên.<br>- Phân quyền Trưởng nhóm (đổi tên phòng, xóa thành viên, tự động chuyển quyền khi rời phòng).<br>- Lịch sử trò chuyện phân trang, hỗ trợ thu hồi tin nhắn. |
| **Tệp đính kèm** | - Upload ảnh, file ghi âm giọng nói (Voice Message) lên server (giới hạn tối đa 50MB).<br>- Tải và hiển thị ảnh, phát âm thanh trực tiếp trên giao diện Swing. |
| **Dữ liệu cá nhân** | - CRUD bản ghi dữ liệu cá nhân theo người dùng sở hữu. |
| **Quản trị hệ thống** | - Xem danh sách người dùng, thay đổi vai trò (`ROLE_USER` / `ROLE_ADMIN`), khóa/xóa tài khoản.<br>- Thống kê lượt request và theo dõi nhật ký lỗi ngoại lệ (`ErrorLog`). |
| **Giám sát & Logging** | - Ghi nhận mọi HTTP Request (`RequestLog`) gồm IP, URL, Method, Username, Thời gian phản hồi.<br>- Tự động kích hoạt tác vụ Cron dọn dẹp log quá hạn (mặc định lưu 30 ngày). |

---

## Cài đặt và Khởi chạy

### 1. Yêu cầu môi trường
- **Java Development Kit (JDK)**: Phiên bản **Java 17** trở lên.
- **MySQL Server**: Phiên bản 8.0 trở lên đang chạy cổng `3306`.
- **IDE khuyến nghị**: IntelliJ IDEA, Eclipse hoặc NetBeans.

### 2. Tạo cơ sở dữ liệu MySQL
Mở MySQL Workbench, DBeaver hoặc Terminal chạy lệnh:
```sql
CREATE DATABASE networkdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Biến môi trường khuyến nghị
Mặc định hệ thống sử dụng cấu hình trong `network-backend/src/main/resources/application.properties`. Bạn có thể thiết lập biến môi trường tương ứng:

| Biến môi trường | Mô tả | Giá trị mặc định |
|---|---|---|
| `DB_USERNAME` | Tài khoản MySQL | `root` |
| `DB_PASSWORD` | Mật khẩu MySQL | `admin` |
| `JWT_SECRET` | Khóa bí mật ký JWT | `dev-only-jwt-secret-change-me-...` |
| `ADMIN_PASSWORD` | Mật khẩu tài khoản admin khởi tạo | `admin123` |
| `MAIL_USERNAME` | Gmail dùng gửi mã OTP | Trống *(in ra console nếu chưa cấu hình)* |
| `MAIL_PASSWORD` | Mật khẩu ứng dụng 16 ký tự Gmail | Trống |

---

### 4. Khởi chạy Backend (Spring Boot)

#### Cách 1: Chạy bằng Gradle Wrapper từ dòng lệnh
```powershell
# Chạy trực tiếp module network-backend
.\gradlew.bat :network-backend:bootRun

# Hoặc trên Linux / macOS:
./gradlew :network-backend:bootRun
```

#### Cách 2: Chạy trực tiếp từ IntelliJ IDEA
1. Mở thư mục gốc `network` bằng IntelliJ IDEA.
2. Đợi Gradle đồng bộ (Sync) xong.
3. Tìm đến file `network-backend/src/main/java/com/example/network/NetworkApplication.java`.
4. Nhấn nút **Run** (biểu tượng tam giác xanh).
5. Server sẽ khởi động tại địa chỉ: `http://localhost:8080`.

---

### 5. Khởi chạy Desktop Client (ChatWave - Java Swing)

#### Cách 1: Chạy từ IntelliJ IDEA
1. Trong cửa sổ Project bên trái, duyệt đến:  
   `clientswing` ➜ `clientswing` ➜ `src`.
2. Chuột phải vào thư mục `src` ➜ chọn **Mark Directory as** ➜ **Sources Root** (nếu chưa được đánh dấu).
3. Mở file `src/chatwave/Main.java`.
4. Chuột phải chọn **Run 'Main.main()'**.

#### Cách 2: Biên dịch và chạy bằng dòng lệnh (PowerShell)
```powershell
# Di chuyển vào thư mục clientswing
cd clientswing\clientswing

# Tạo thư mục classes và biên dịch toàn bộ source code
New-Item -ItemType Directory -Force -Path "build\classes"
$files = (Get-ChildItem -Recurse -Filter "*.java" src).FullName
javac -encoding UTF-8 -d "build\classes" $files

# Khởi chạy ứng dụng ChatWave
java -cp "build\classes" chatwave.Main
```

#### Cách 3: Mở bằng NetBeans IDE
1. Khởi động NetBeans IDE.
2. Chọn **File** ➜ **Open Project** ➜ chọn thư mục `clientswing/clientswing`.
3. Bấm **Clean and Build**, sau đó nhấn **Run** (`F6`).

---

### 6. Giao diện Web Client Demo (Legacy)
Sau khi Backend khởi động, bạn có thể truy cập các trang tĩnh qua trình duyệt:
- **Ứng dụng Web Chat**: [http://localhost:8080/index.html](http://localhost:8080/index.html)
- **Trang Quản trị Admin**: [http://localhost:8080/admin.html](http://localhost:8080/admin.html)

---

## Tài khoản mặc định

Hệ thống được cấu hình tự động tạo tài khoản Quản trị viên (`ADMIN`) khi khởi động lần đầu nếu cơ sở dữ liệu chưa có:

| Tên đăng nhập | Mật khẩu mặc định | Vai trò (Role) | Chú thích |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Có toàn quyền truy cập trang quản trị, đổi role user, xem log |

---

## Cấu hình gửi email quên mật khẩu

Hệ thống hỗ trợ gửi mã xác thực OTP 6 số qua giao thức Gmail SMTP:
1. Đăng nhập Google ➜ **Quản lý Tài khoản Google** ➜ tab **Bảo mật**.
2. Kích hoạt **Xác minh 2 bước (2-Step Verification)**.
3. Tạo **Mật khẩu ứng dụng (App Password)** cho ứng dụng `Mini Messenger`.
4. Sao chép mật khẩu 16 chữ số và gán vào biến môi trường:
   - `MAIL_USERNAME=email_cua_ban@gmail.com`
   - `MAIL_PASSWORD=mat_khau_16_ky_tu`

> **Lưu ý khi test (Chế độ Dev):** Nếu bạn không cấu hình email, server sẽ tự động in mã OTP trực tiếp ra màn hình Console log của server để tiện kiểm thử.

---

## Danh mục RESTful API

Tất cả các API nghiệp vụ đều có tiền tố `/api`. Trừ các endpoint công khai (Đăng ký, Đăng nhập, Quên mật khẩu), mọi request bắt buộc đính kèm header:  
`Authorization: Bearer <token_jwt>`

### 1. Xác thực – `/api/auth` (Công khai)
- `POST /api/auth/register`: Đăng ký tài khoản mới.
- `POST /api/auth/login`: Đăng nhập, nhận JWT Token và thông tin User.
- `POST /api/auth/forgot-password`: Yêu cầu cấp mã OTP qua email (Body: `{"email": "..."}`).
- `POST /api/auth/reset-password`: Đặt lại mật khẩu mới bằng OTP (Body: `{"email": "...", "code": "...", "newPassword": "..."}`).

### 2. Hồ sơ cá nhân – `/api/profile`
- `GET /api/profile/me`: Lấy thông tin hồ sơ của tài khoản đang đăng nhập.
- `PUT /api/profile/me`: Cập nhật thông tin hồ sơ của mình.
- `GET /api/profile/{username}`: Xem thông tin công khai của người dùng khác.

### 3. Bạn bè – `/api/friends`
- `GET /api/friends`: Lấy danh sách bạn bè hiện tại.
- `POST /api/friends/request`: Gửi lời mời kết bạn (Body: `{"receiverUsername": "..."}`).
- `GET /api/friends/requests/pending`: Danh sách lời mời đang chờ phản hồi.
- `POST /api/friends/requests/{id}/accept`: Chấp nhận lời mời kết bạn.
- `POST /api/friends/requests/{id}/decline`: Từ chối lời mời kết bạn.
- `DELETE /api/friends/{friendUsername}`: Hủy kết bạn.

### 4. Tin nhắn riêng 1-1 – `/api/direct-messages`
- `GET /api/direct-messages/conversations`: Lấy danh sách các cuộc trò chuyện gần nhất.
- `GET /api/direct-messages/{friendUsername}`: Lấy lịch sử chat 1-1 (hỗ trợ phân trang `page`, `size`).
- `POST /api/direct-messages`: Gửi tin nhắn riêng (Body: `{"receiverUsername": "...", "content": "..."}`).
- `PUT /api/direct-messages/{id}/read`: Đánh dấu tin nhắn đã đọc.
- `PUT /api/direct-messages/{id}/recall`: Thu hồi tin nhắn (chỉ người gửi).
- `DELETE /api/direct-messages/{id}/for-me`: Xóa tin nhắn ở phía hiển thị của tôi.

### 5. Phòng chat nhóm – `/api/rooms`
- `GET /api/rooms`: Danh sách các phòng chat bản thân đang tham gia.
- `POST /api/rooms`: Tạo phòng chat mới (Body: `{"name": "...", "initialMembers": [...]}`).
- `GET /api/rooms/{id}/members`: Xem danh sách thành viên trong phòng.
- `POST /api/rooms/{id}/members`: Thêm bạn bè vào phòng chat.
- `PUT /api/rooms/{id}/name`: Đổi tên phòng chat.
- `POST /api/rooms/{id}/leave`: Rời khỏi phòng (tự động chuyển quyền cho thành viên khác nếu là trưởng nhóm).
- `DELETE /api/rooms/{id}/members/{username}`: Trưởng nhóm xóa thành viên khỏi phòng.
- `GET /api/rooms/{roomId}/messages`: Xem lịch sử tin nhắn phòng (phân trang).
- `POST /api/rooms/{roomId}/messages`: Gửi tin nhắn vào phòng.
- `PUT /api/rooms/{roomId}/messages/{messageId}/recall`: Thu hồi tin nhắn phòng.
- `DELETE /api/rooms/{roomId}/messages/{messageId}/for-me`: Xóa tin nhắn phòng ở phía tôi.

### 6. File đính kèm & Tải lên – `/api/files`
- `POST /api/files/upload`: Tải lên ảnh / file âm thanh (Multipart Form-Data, tham số `file`).
- `GET /api/files/{fileName}`: Xem / tải tệp đã lưu trữ trên server.

### 7. Dữ liệu cá nhân – `/api/data`
- `GET /api/data`: Lấy danh sách bản ghi dữ liệu của tôi.
- `GET /api/data/{id}`: Xem chi tiết một bản ghi.
- `POST /api/data`: Tạo bản ghi mới.
- `PUT /api/data/{id}`: Chỉnh sửa bản ghi.
- `DELETE /api/data/{id}`: Xóa bản ghi.

### 8. Quản trị & Giám sát – `/api/admin` & `/api/logs` (Chỉ quyền `ADMIN`)
- `GET /api/admin/users`: Danh sách toàn bộ tài khoản người dùng trong hệ thống.
- `PUT /api/admin/users/{id}/role`: Phân quyền người dùng (`USER` hoặc `ADMIN`).
- `DELETE /api/admin/users/{id}`: Xóa tài khoản người dùng khỏi hệ thống.
- `GET /api/admin/error-logs`: Xem nhật ký lỗi hệ thống (`ErrorLog`).
- `GET /api/logs`: Xem nhật ký truy vết HTTP Request (`RequestLog`).

---

## Giao thức WebSocket STOMP

- **Endpoint kết nối**: `/ws`
- **Xác thực kết nối**: Header `Authorization: Bearer <token_jwt>` được truyền trong frame STOMP `CONNECT`.
- **Gửi tin nhắn vào phòng**: `/app/chat.send/{roomId}` với payload JSON: `{"content": "..."}`.
- **Lắng nghe tin phòng**: Subscribe `/topic/room.{roomId}` *(kiểm tra quyền thành viên)*.
- **Lắng nghe tin riêng 1-1**: Subscribe `/topic/direct.{username}`.
- **Kênh thông báo lỗi**: `/user/queue/errors`.

---

## Cơ chế bảo mật và Giám sát

1. **Phân quyền chặt chẽ (Spring Security + JWT)**:
   - Các API công khai được whitelist qua cấu hình `SecurityConfig`.
   - `JwtAuthFilter` kiểm tra và nạp ngữ cảnh bảo mật (`SecurityContextHolder`) cho từng request.
2. **Chống dò quét tài khoản (LoginRateLimiter)**:
   - Giới hạn số lần thử mật khẩu sai: quá 5 lần trong 15 phút sẽ tạm khóa yêu cầu trong 5 phút.
3. **Giám sát lưu vết toàn diện**:
   - `RequestLoggingFilter` tự động chặn mọi request để ghi nhận IP, URI, Phương thức, Trạng thái HTTP, Thời gian phản hồi tính theo millisecond vào bảng `request_logs`.
   - Lỗi ngoại lệ nghiệp vụ và lỗi hệ thống được `GlobalExceptionHandler` bắt tập trung và ghi vào bảng `error_logs`.
   - Tác vụ định kỳ `RequestLogCleanup` tự động quét và loại bỏ dữ liệu log cũ hơn 30 ngày để tiết kiệm dung lượng lưu trữ.
