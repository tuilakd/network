package com.example.network.entity;

import com.example.network.entity.enums.FriendShipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity lưu trữ mối quan hệ bạn bè hoặc lời mời kết bạn.
 * Status: PENDING, ACCEPTED, DECLINED.
 */
@Entity
@Table(name = "friendships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"requester", "addressee"})
})
@Getter
@Setter
@NoArgsConstructor
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String requester;

    @Column(nullable = false)
    private String addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendShipStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}