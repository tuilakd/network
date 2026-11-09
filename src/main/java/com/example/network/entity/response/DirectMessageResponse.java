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
    private LocalDateTime sentAt;

    public static DirectMessageResponse from(DirectMessage dm) {
        return new DirectMessageResponse(
                dm.getId(),
                dm.getSender(),
                dm.getRecipient(),
                dm.getContent(),
                dm.getAttachmentUrl(),
                dm.getAttachmentType(),
                dm.isRead(),
                dm.getSentAt()
        );
    }
}
