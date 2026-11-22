package com.example.network.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity ghi nhận tin nhắn phòng chat đã bị người dùng xóa ở phía họ (Delete for me).
 */
@Entity
@Table(name = "chat_message_deletions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"messageId", "username"}),
        indexes = @Index(name = "idx_cmd_user_msg", columnList = "username, messageId"))
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDeletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false)
    private String username;

    private LocalDateTime deletedAt;

    public ChatMessageDeletion(Long messageId, String username) {
        this.messageId = messageId;
        this.username = username;
        this.deletedAt = LocalDateTime.now();
    }
}
