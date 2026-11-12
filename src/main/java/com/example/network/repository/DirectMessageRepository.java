package com.example.network.repository;

import com.example.network.entity.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    @Query("SELECT dm FROM DirectMessage dm WHERE " +
            "(dm.sender = :u1 AND dm.recipient = :u2) OR " +
            "(dm.sender = :u2 AND dm.recipient = :u1) " +
            "ORDER BY dm.sentAt DESC")
    List<DirectMessage> findBetweenUsers(@Param("u1") String u1, @Param("u2") String u2, Pageable pageable);

    long countBySenderAndRecipientAndIsReadFalse(String sender, String recipient);
    void deleteBySenderOrRecipient(String sender, String recipient);
}
