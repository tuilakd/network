package com.example.network.entity.response;

import com.example.network.entity.ChatMessage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {

    private Long id;
    private Long roomId;
    private String sender;
    private String content;
    private LocalDateTime sentAt;

    public static ChatMessageResponse from(ChatMessage m) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.id = m.getId();
        r.roomId = m.getRoomId();
        r.sender = m.getSender();
        r.content = m.getContent();
        r.sentAt = m.getSentAt();
        return r;
    }
}