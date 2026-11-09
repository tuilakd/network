package com.example.network.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi tập trung cho toàn bộ ứng dụng.
 */
@Getter
public enum ErrorCode {

    // Lỗi 400 Bad Request
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    INVALID_INPUT("INVALID_INPUT", HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không đúng định dạng"),
    CANNOT_ADD_SELF("CANNOT_ADD_SELF", HttpStatus.BAD_REQUEST, "Không thể tự kết bạn với chính mình"),
    CANNOT_CHAT_WITH_SELF("CANNOT_CHAT_WITH_SELF", HttpStatus.BAD_REQUEST, "Không thể tự nhắn tin cho chính mình"),
    CANNOT_CHANGE_OWN_ROLE("CANNOT_CHANGE_OWN_ROLE", HttpStatus.BAD_REQUEST, "Không thể tự thay đổi quyền của chính mình"),
    CANNOT_DELETE_SELF("CANNOT_DELETE_SELF", HttpStatus.BAD_REQUEST, "Không thể tự xóa tài khoản của chính mình"),

    // Lỗi 401 Unauthorized
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên làm việc đã hết hạn"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu"),

    // Lỗi 403 Forbidden
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    NOT_FRIENDS("NOT_FRIENDS", HttpStatus.FORBIDDEN, "Hai người phải là bạn bè mới có thể nhắn tin"),
    NOT_ROOM_MEMBER("NOT_ROOM_MEMBER", HttpStatus.FORBIDDEN, "Bạn chưa tham gia phòng chat này"),
    NOT_MESSAGE_RECIPIENT("NOT_MESSAGE_RECIPIENT", HttpStatus.FORBIDDEN, "Bạn không phải là người nhận tin nhắn này"),

    // Lỗi 404 Not Found
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy phòng chat"),
    MESSAGE_NOT_FOUND("MESSAGE_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tin nhắn"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu"),

    // Lỗi 409 Conflict
    USERNAME_EXISTS("USERNAME_EXISTS", HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại"),
    EMAIL_EXISTS("EMAIL_EXISTS", HttpStatus.CONFLICT, "Email đã được sử dụng"),
    ROOM_NAME_EXISTS("ROOM_NAME_EXISTS", HttpStatus.CONFLICT, "Tên phòng chat đã tồn tại"),
    ALREADY_ROOM_MEMBER("ALREADY_ROOM_MEMBER", HttpStatus.CONFLICT, "Bạn đã ở trong phòng này rồi"),
    ALREADY_FRIENDS("ALREADY_FRIENDS", HttpStatus.CONFLICT, "Hai người đã là bạn bè của nhau"),
    FRIEND_REQUEST_PENDING("FRIEND_REQUEST_PENDING", HttpStatus.CONFLICT, "Lời mời kết bạn đang chờ phản hồi"),

    // Lỗi 500 Internal Server Error
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
