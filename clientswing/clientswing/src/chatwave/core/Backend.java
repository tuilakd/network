package chatwave.core;

import chatwave.core.Models.*;
import java.io.File;
import java.util.List;

/**
 * Toàn bộ những gì giao diện cần từ máy chủ. Có 2 bản cài đặt:
 * RemoteBackend (gọi REST API Spring Boot thật) và DemoBackend (dữ liệu giả để xem giao diện).
 * Nếu backend của nhóm đổi đường dẫn / JSON thì chỉ cần sửa RemoteBackend.
 */
public interface Backend {
    Account login(String username, String password) throws Exception;
    void register(String username, String email, String password) throws Exception;
    void logout();
    /** Quên mật khẩu: backend gửi mã xác nhận tới email. */
    void forgotPassword(String email) throws Exception;
    void resetPassword(String email, String code, String newPassword) throws Exception;

    List<Friend> friends() throws Exception;
    List<Req> pendingRequests() throws Exception;
    void sendRequest(String targetUsername) throws Exception;
    void accept(long requestId) throws Exception;
    void decline(long requestId) throws Exception;
    void unfriend(String username) throws Exception;

    List<Msg> dmHistory(String friend) throws Exception;
    Msg sendDm(String to, String content, String attUrl, String attType) throws Exception;
    void markRead(long messageId) throws Exception;
    /** Thu hồi tin nhắn riêng do chính mình gửi. */
    void recallDm(long messageId) throws Exception;
    /** Thu hồi tin nhắn trong nhóm do chính mình gửi. */
    void recallRoom(long roomId, long messageId) throws Exception;

    List<Room> myRooms() throws Exception;
    List<Room> allRooms() throws Exception;
    Room createRoom(String name) throws Exception;
    void joinRoom(long roomId) throws Exception;
    List<String> roomMembers(long roomId) throws Exception;
    List<Msg> roomHistory(long roomId) throws Exception;
    Msg sendRoom(long roomId, String content, String attUrl, String attType) throws Exception;

    Upload upload(File file) throws Exception;
    byte[] download(String url) throws Exception;

    Profile profile(String username) throws Exception;
    Profile updateMyProfile(String fullName, String bio) throws Exception;

    // ----- quản trị (ROLE_ADMIN) -----
    List<AdminUser> adminUsers() throws Exception;
    void adminSetRole(long userId, String role) throws Exception;
    void adminDeleteUser(long userId) throws Exception;
    List<ReqLog> requestLogs(int size) throws Exception;
    List<ErrLog> errorLogs(int size) throws Exception;
}
