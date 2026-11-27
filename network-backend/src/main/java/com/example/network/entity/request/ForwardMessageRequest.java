package com.example.network.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO yêu cầu chuyển tiếp một tin nhắn tới nhiều bạn bè hoặc nhóm.
 */
@Getter
@Setter
public class ForwardMessageRequest {

    @NotBlank(message = "Loại tin nhắn gốc không được để trống (DM hoặc ROOM)")
    private String sourceType;

    private List<String> friendUsernames;

    private List<Long> roomIds;
}
