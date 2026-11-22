package com.example.network.entity.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO tóm tắt cuộc trò chuyện 1-1 gần nhất với bạn bè.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String friendUsername;
    private String lastMessage;
    private String lastSender;
    private LocalDateTime lastSentAt;
    private long unreadCount;
}
