package com.example.network.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity lưu mối quan hệ thành viên trong phòng chat.
 */
@Entity
@Table(name = "room_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"roomId", "username"}))
@Getter
@Setter
@NoArgsConstructor
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private String username;

    private LocalDateTime joinedAt;

    @PrePersist
    public void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}