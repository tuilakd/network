package com.example.network.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMemberRequest {

    @NotBlank(message = "Tên người dùng không được để trống")
    private String username;
}
