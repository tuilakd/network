package com.example.network.entity.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO yêu cầu gửi mã OTP quên mật khẩu.
 */
@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
}
