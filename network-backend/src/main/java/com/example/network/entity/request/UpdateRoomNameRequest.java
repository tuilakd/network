package com.example.network.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoomNameRequest {

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(min = 2, max = 50, message = "Tên phòng phải từ 2 đến 50 ký tự")
    private String name;
}
