package com.example.network.repository;

import com.example.network.entity.ChatMessageDeletion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageDeletionRepository extends JpaRepository<ChatMessageDeletion, Long> {
    boolean existsByMessageIdAndUsername(Long messageId, String username);
    void deleteByMessageId(Long messageId);
    void deleteByUsername(String username);
}
