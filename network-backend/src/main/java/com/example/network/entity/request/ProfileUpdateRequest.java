package com.example.network.entity.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO yêu cầu cập nhật hồ sơ cá nhân.
 */
@Getter
@Setter
public class ProfileUpdateRequest {

    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @Size(max = 100, message = "Giới thiệu tối đa 100 ký tự")
    private String bio;

    private String avatarUrl;
}
