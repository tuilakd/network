package com.example.network.exception;

import lombok.Getter;

/**
 * Danh mục mã lỗi tập trung cho toàn bộ ứng dụng.
 */
@Getter
public enum ErrorCode {

    // Lỗi 400 Bad Request
    BAD_REQUEST("NET-001", "Yêu cầu không hợp lệ"),
    INVALID_INPUT("NET-002", "Dữ liệu đầu vào không đúng định dạng"),
    CANNOT_ADD_SELF("NET-003", "Không thể tự kết bạn với chính mình"),
    CANNOT_CHAT_WITH_SELF("NET-004", "Không thể tự nhắn tin cho chính mình"),
    CANNOT_CHANGE_OWN_ROLE("NET-005", "Không thể tự thay đổi quyền của chính mình"),
    CANNOT_DELETE_SELF("NET-006", "Không thể tự xóa tài khoản của chính mình"),

    // Lỗi 401 Unauthorized
    UNAUTHORIZED("NET-007", "Chưa đăng nhập hoặc phiên làm việc đã hết hạn"),
    INVALID_CREDENTIALS("NET-008", "Sai tên đăng nhập hoặc mật khẩu"),

    // Lỗi 403 Forbidden
    FORBIDDEN("NET-009", "Bạn không có quyền thực hiện thao tác này"),
    NOT_FRIENDS("NET-010", "Hai người phải là bạn bè mới có thể nhắn tin"),
    NOT_ROOM_MEMBER("NET-011", "Bạn chưa tham gia phòng chat này"),
    NOT_MESSAGE_RECIPIENT("NET-012", "Bạn không phải là người nhận tin nhắn này"),

    // Lỗi 404 Not Found
    USER_NOT_FOUND("NET-013", "Không tìm thấy người dùng"),
    ROOM_NOT_FOUND("NET-014", "Không tìm thấy phòng chat"),
    MESSAGE_NOT_FOUND("NET-015", "Không tìm thấy tin nhắn"),
    RESOURCE_NOT_FOUND("NET-016", "Không tìm thấy tài nguyên yêu cầu"),

    // Lỗi 409 Conflict
    USERNAME_EXISTS("NET-017", "Tên đăng nhập đã tồn tại"),
    EMAIL_EXISTS("NET-018", "Email đã được sử dụng"),
    ROOM_NAME_EXISTS("NET-019", "Tên phòng chat đã tồn tại"),
    ALREADY_ROOM_MEMBER("NET-020", "Bạn đã ở trong phòng này rồi"),
    ALREADY_FRIENDS("NET-021", "Hai người đã là bạn bè của nhau"),
    FRIEND_REQUEST_PENDING("NET-022", "Lời mời kết bạn đang chờ phản hồi"),

    // Lỗi 500 Internal Server Error
    INTERNAL_SERVER_ERROR("NET-023", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}