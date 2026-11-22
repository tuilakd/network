package com.example.network.repository;

import com.example.network.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId " +
            "AND NOT EXISTS (SELECT 1 FROM ChatMessageDeletion cmd WHERE cmd.messageId = m.id AND cmd.username = :username) " +
            "ORDER BY m.sentAt DESC")
    List<ChatMessage> findByRoomIdVisibleForUser(@Param("roomId") Long roomId, @Param("username") String username, Pageable pageable);

    void deleteBySender(String sender);
    void deleteByRoomId(Long roomId);
}