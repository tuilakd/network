package com.example.network.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO gửi tin nhắn riêng 1-1.
 */
@Getter
@Setter
public class DirectMessageRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipient;

    @Size(max = 2000, message = "Nội dung tin nhắn tối đa 2000 ký tự")
    private String content;

    private String attachmentUrl;

    private String attachmentType;
}
