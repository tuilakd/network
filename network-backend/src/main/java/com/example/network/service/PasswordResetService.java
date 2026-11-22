package com.example.network.service;

import com.example.network.entity.request.ForgotPasswordRequest;
import com.example.network.entity.request.ResetPasswordRequest;
import com.example.network.entity.response.ApiMessageResponse;

/**
 * Service xử lý nghiệp vụ quên mật khẩu và đặt lại mật khẩu bằng mã OTP.
 */
public interface PasswordResetService {

    /**
     * Tạo mã OTP 6 số và gửi qua email cho người dùng (nếu hợp lệ).
     *
     * @param request thông tin email cần lấy lại mật khẩu
     * @return thông báo phản hồi chung
     */
    ApiMessageResponse forgotPassword(ForgotPasswordRequest request);

    /**
     * Xác thực mã OTP và đặt lại mật khẩu mới.
     *
     * @param request thông tin email, mã OTP và mật khẩu mới
     * @return thông báo kết quả đặt lại mật khẩu
     */
    ApiMessageResponse resetPassword(ResetPasswordRequest request);
}
