package com.example.network.entity.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO yêu cầu đặt lại mật khẩu với mã OTP xác nhận.
 */
@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mã xác nhận không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã xác nhận phải gồm đúng 6 chữ số")
    private String code;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;
}
