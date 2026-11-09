package com.example.network.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO yêu cầu kết bạn.
 */
@Getter
@Setter
public class FriendRequest {

    @NotBlank(message = "Tên tài khoản người nhận không được để trống")
    private String targetUsername;
}
