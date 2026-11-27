package com.example.network.service.impl;

import com.example.network.entity.*;
import com.example.network.entity.enums.FriendShipStatus;
import com.example.network.entity.response.ChatMessageResponse;
import com.example.network.entity.response.DirectMessageResponse;
import com.example.network.exception.AppException;
import com.example.network.exception.code.DomainCode;
import com.example.network.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

/**
 * Xử lý tạo bản ghi tin nhắn chuyển tiếp cho từng nơi nhận trong transaction độc lập (REQUIRES_NEW).
 */
@Service
@RequiredArgsConstructor
public class ForwardTargetService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final DirectMessageRepository directMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forwardToFriend(String sender, Long sourceMsgId, String content, String attUrl, String attType, String targetUsername) {
        if (targetUsername == null || targetUsername.isBlank()) {
            throw new AppException(DomainCode.INVALID_INPUT, "Tên người nhận không hợp lệ");
        }
        String trimmed = targetUsername.trim();
        if (sender.equalsIgnoreCase(trimmed)) {
            throw new AppException(DomainCode.CANNOT_CHAT_WITH_SELF);
        }
        User targetUser = userRepository.findByUsername(trimmed)
                .orElseThrow(() -> new AppException(DomainCode.USER_NOT_FOUND));
        String canonicalSender = userRepository.findByUsername(sender).map(User::getUsername).orElse(sender);
        String canonicalTarget = targetUser.getUsername();

        Optional<Friendship> fs = friendshipRepository.findBetweenUsers(canonicalSender, canonicalTarget);
        if (fs.isEmpty() || fs.get().getStatus() != FriendShipStatus.ACCEPTED) {
            throw new AppException(DomainCode.NOT_FRIENDS);
        }

        DirectMessage dm = new DirectMessage();
        dm.setSender(canonicalSender);
        dm.setRecipient(canonicalTarget);
        dm.setContent(content == null ? "" : content);
        dm.setAttachmentUrl(attUrl);
        dm.setAttachmentType(attType);
        dm.setRead(false);
        dm.setForwarded(true);
        dm.setForwardedFromMessageId(sourceMsgId);
        DirectMessage saved = directMessageRepository.save(dm);

        DirectMessageResponse resp = DirectMessageResponse.from(saved);
        afterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/direct." + canonicalTarget, resp);
            messagingTemplate.convertAndSend("/topic/direct." + canonicalSender, resp);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forwardToRoom(String sender, Long sourceMsgId, String content, String attUrl, String attType, Long roomId) {
        if (roomId == null || !chatRoomRepository.existsById(roomId)) {
            throw new AppException(DomainCode.ROOM_NOT_FOUND);
        }
        if (!roomMemberRepository.existsByRoomIdAndUsername(roomId, sender)) {
            throw new AppException(DomainCode.NOT_ROOM_MEMBER);
        }
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setSender(sender);
        msg.setContent(content == null ? "" : content);
        msg.setAttachmentUrl(attUrl);
        msg.setAttachmentType(attType);
        msg.setForwarded(true);
        msg.setForwardedFromMessageId(sourceMsgId);
        ChatMessage saved = chatMessageRepository.save(msg);

        ChatMessageResponse resp = ChatMessageResponse.from(saved);
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/room." + roomId, resp));
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
