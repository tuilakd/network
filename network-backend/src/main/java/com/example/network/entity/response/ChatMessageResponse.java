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
    private String attachmentUrl;
    private String attachmentType;
    private boolean isRecalled;
    private boolean forwarded;
    private LocalDateTime recalledAt;
    private LocalDateTime sentAt;

    public static ChatMessageResponse from(ChatMessage m) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.id = m.getId();
        r.roomId = m.getRoomId();
        r.sender = m.getSender();
        r.isRecalled = m.isRecalled();
        r.forwarded = m.isForwarded();
        r.recalledAt = m.getRecalledAt();
        if (m.isRecalled()) {
            r.content = "Tin nhắn đã được thu hồi";
            r.attachmentUrl = null;
            r.attachmentType = null;
        } else {
            r.content = m.getContent();
            r.attachmentUrl = m.getAttachmentUrl();
            r.attachmentType = m.getAttachmentType();
        }
        r.sentAt = m.getSentAt();
        return r;
    }
}