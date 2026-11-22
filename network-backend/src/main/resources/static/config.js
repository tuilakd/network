/* ==========================================================
 * CẤU HÌNH KẾT NỐI BACKEND (Spring Boot - NetBeans)
 * Chỉ cần sửa file này khi ghép với backend của nhóm.
 * ========================================================== */
window.CONFIG = {
  // true  = chạy thử không cần backend (dữ liệu giả lưu trong trình duyệt)
  // false = gọi API thật của Spring Boot
  DEMO_MODE: false,

  // Chỉ có tác dụng khi DEMO_MODE = true.
  // false = chỉ có tài khoản người thật tự đăng ký (mở 2 tab, mỗi tab một tài khoản để thử kết bạn / nhắn tin)
  // true  = thêm 4 tài khoản "máy" an / binh / chi / dung (mật khẩu: bot): tự chấp nhận kết bạn,
  //         tự xem và trả lời tin nhắn, tiện thử khi chỉ có một mình
  DEMO_BOTS: false,

  // Địa chỉ backend Spring Boot
  API_BASE: "/api",

  // Gốc địa chỉ để ghép đường dẫn file ảnh/video backend trả về
  // (ví dụ backend trả "/uploads/abc.jpg" -> http://localhost:8080/uploads/abc.jpg)
  FILE_BASE: "",

  // "tab"     = mỗi tab trình duyệt có phiên đăng nhập riêng (mở 2 tab để thử 2 tài khoản cùng lúc,
  //             đóng tab là đăng xuất)
  // "browser" = nhớ đăng nhập cho cả trình duyệt (kể cả sau khi tắt đi mở lại)
  SESSION_SCOPE: "tab",

  // Chu kỳ tự tải tin nhắn mới, danh sách bạn bè, lời mời, tin chưa đọc (mili giây)
  POLL_MS: 3000,

  // Số ký tự tối thiểu để bắt đầu tìm kiếm tài khoản mới
  MIN_SEARCH_CHARS: 3,

  // Giới hạn dung lượng tệp gửi đi (MB)
  MAX_IMAGE_MB: 10,
  MAX_VIDEO_MB: 50,
  MAX_AUDIO_MB: 10,

  // Chức năng chỉ dùng khi DEMO_MODE = false. Đặt true khi backend đã có endpoint tương ứng.
  FEATURES: {
    forgotPassword: true,  // quên mật khẩu (backend đã có)
    editEmail: false,       // thêm/đổi email sau đăng ký (backend chưa có)
    displayName: false,     // ô "Tên hiển thị" khi đăng ký (backend chỉ có username)
  },

  // Đường dẫn các endpoint - đổi cho khớp với Controller của nhóm
  ENDPOINTS: {
    login:    "/auth/login",           // POST  {username, password}
    register: "/auth/register",        // POST  {username, password, displayName, email}
    forgotPassword: "/auth/forgot-password", // POST {email}  -> backend gửi mã 6 số qua email
    resetPassword:  "/auth/reset-password",  // POST {email, code, newPassword}

    myEmail: "/users/me/email",       // PUT {email}  thêm/đổi email của chính mình
    searchUsers:    "/users/search",   // GET   ?q=tu-khoa  (chỉ gọi khi người dùng đã gõ tên)
    friends:        "/friends",        // GET danh sách bạn bè | DELETE /{userId} hủy kết bạn
    friendRequests: "/friends/requests", // GET ?type=received|sent | POST {userId} gửi lời mời
                                         // POST /{id}/accept chấp nhận | POST /{id}/decline từ chối
                                         // DELETE /{id} hủy lời mời đã gửi   (id = mã lời mời)

    messages: "/direct-messages",             // GET /messages/{userId} | POST /messages {receiverId, content}
                                       // POST /messages/{userId}/read  đánh dấu đã xem
    upload:   "/files/upload",         // POST multipart "file", rồi gửi tin kèm attachmentUrl
    conversations: "/direct-messages/conversations",
    rooms: "/rooms",                   // GET tất cả | /mine | POST tạo | /{id}/join | /{id}/members | /{id}/messages
    friendRequestSend: "/friends/request",   // POST {targetUsername}
    friendsPending: "/friends/requests/pending",
    profile: "/profile",               // GET /profile/{username}      // POST  multipart: receiverId, file, content (tùy chọn)

    // ----- Khu vực quản trị (admin.html) -----
    adminLogin:    "/auth/login",      // dùng chung đăng nhập, kết quả phải có role = ADMIN
    adminStats:    "/admin/stats",     // GET
    adminUsers:    "/admin/users",     // GET | PUT /{id}/lock {locked} | DELETE /{id}
    adminLogs:     "/logs",      // GET  nhật ký request
  },
};
