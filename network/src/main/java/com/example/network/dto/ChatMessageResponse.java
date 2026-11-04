package com.example.network.dto;

import com.example.network.entity.ChatMessage;
import java.time.LocalDateTime;

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

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
}