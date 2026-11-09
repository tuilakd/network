package com.example.network.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity lưu tin nhắn riêng tư 1-1 giữa hai bạn bè.
 */
@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "idx_dm_sender_recipient", columnList = "sender, recipient"),
        @Index(name = "idx_dm_sent_at", columnList = "sentAt")
})
@Getter
@Setter
@NoArgsConstructor
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false, length = 2000)
    private String content = "";

    private String attachmentUrl;

    private String attachmentType;

    @Column(nullable = false)
    private boolean isRead = false;

    private LocalDateTime sentAt;

    @PrePersist
    public void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }
}
