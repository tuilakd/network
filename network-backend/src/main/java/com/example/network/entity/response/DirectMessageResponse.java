package com.example.network.entity.response;

import com.example.network.entity.DirectMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin tin nhắn riêng 1-1 kèm file đính kèm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessageResponse {

    private Long id;
    private String sender;
    private String recipient;
    private String content;
    private String attachmentUrl;
    private String attachmentType;
    private boolean isRead;
    private boolean isRecalled;
    private LocalDateTime recalledAt;
    private LocalDateTime sentAt;

    public static DirectMessageResponse from(DirectMessage dm) {
        DirectMessageResponse r = new DirectMessageResponse();
        r.setId(dm.getId());
        r.setSender(dm.getSender());
        r.setRecipient(dm.getRecipient());
        r.setRead(dm.isRead());
        r.setRecalled(dm.isRecalled());
        r.setRecalledAt(dm.getRecalledAt());
        if (dm.isRecalled()) {
            r.setContent("Tin nhắn đã được thu hồi");
            r.setAttachmentUrl(null);
            r.setAttachmentType(null);
        } else {
            r.setContent(dm.getContent());
            r.setAttachmentUrl(dm.getAttachmentUrl());
            r.setAttachmentType(dm.getAttachmentType());
        }
        r.setSentAt(dm.getSentAt());
        return r;
    }
}
