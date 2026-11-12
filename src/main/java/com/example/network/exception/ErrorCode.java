package com.example.network.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    BAD_REQUEST("NET-001", "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_INPUT("NET-002", "Dữ liệu đầu vào không đúng định dạng", HttpStatus.BAD_REQUEST),
    CANNOT_ADD_SELF("NET-003", "Không thể tự kết bạn với chính mình", HttpStatus.BAD_REQUEST),
    CANNOT_CHAT_WITH_SELF("NET-004", "Không thể tự nhắn tin cho chính mình", HttpStatus.BAD_REQUEST),
    CANNOT_CHANGE_OWN_ROLE("NET-005", "Không thể tự thay đổi quyền của chính mình", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_SELF("NET-006", "Không thể tự xóa tài khoản của chính mình", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED("NET-007", "Chưa đăng nhập hoặc phiên làm việc đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("NET-008", "Sai tên đăng nhập hoặc mật khẩu", HttpStatus.UNAUTHORIZED),

    FORBIDDEN("NET-009", "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    NOT_FRIENDS("NET-010", "Hai người phải là bạn bè mới có thể nhắn tin", HttpStatus.FORBIDDEN),
    NOT_ROOM_MEMBER("NET-011", "Bạn chưa tham gia phòng chat này", HttpStatus.FORBIDDEN),
    NOT_MESSAGE_RECIPIENT("NET-012", "Bạn không phải là người nhận tin nhắn này", HttpStatus.FORBIDDEN),

    USER_NOT_FOUND("NET-013", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    ROOM_NOT_FOUND("NET-014", "Không tìm thấy phòng chat", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND("NET-015", "Không tìm thấy tin nhắn", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("NET-016", "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),

    USERNAME_EXISTS("NET-017", "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_EXISTS("NET-018", "Email đã được sử dụng", HttpStatus.CONFLICT),
    ROOM_NAME_EXISTS("NET-019", "Tên phòng chat đã tồn tại", HttpStatus.CONFLICT),
    ALREADY_ROOM_MEMBER("NET-020", "Bạn đã ở trong phòng này rồi", HttpStatus.CONFLICT),
    ALREADY_FRIENDS("NET-021", "Hai người đã là bạn bè của nhau", HttpStatus.CONFLICT),
    FRIEND_REQUEST_PENDING("NET-022", "Lời mời kết bạn đang chờ phản hồi", HttpStatus.CONFLICT),

    TOO_MANY_REQUESTS("NET-024", "Thao tác quá nhiều lần, vui lòng thử lại sau", HttpStatus.TOO_MANY_REQUESTS),

    INTERNAL_SERVER_ERROR("NET-023", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }
}