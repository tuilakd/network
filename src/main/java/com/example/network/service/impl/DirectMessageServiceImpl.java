package com.example.network.service.impl;

import com.example.network.entity.DirectMessage;
import com.example.network.entity.Friendship;
import com.example.network.entity.User;
import com.example.network.entity.request.DirectMessageRequest;
import com.example.network.entity.response.ConversationResponse;
import com.example.network.entity.response.DirectMessageResponse;
import com.example.network.exception.AppException;
import com.example.network.exception.ErrorCode;
import com.example.network.repository.DirectMessageRepository;
import com.example.network.repository.FriendshipRepository;
import com.example.network.repository.UserRepository;
import com.example.network.service.DirectMessageService;
import com.example.network.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public DirectMessageResponse send(String sender, DirectMessageRequest request) {
        if (request == null || request.getRecipient() == null || request.getRecipient().isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Người nhận không được để trống");
        }

        String trimmedRecipient = request.getRecipient().trim();
        if (sender.equalsIgnoreCase(trimmedRecipient)) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        // Lấy User từ DB để chuẩn hóa đúng chữ hoa/thường cho DB và WebSocket topic
        User recipientUser = userRepository.findByUsername(trimmedRecipient)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String canonicalSender = userRepository.findByUsername(sender)
                .map(User::getUsername)
                .orElse(sender);
        String canonicalRecipient = recipientUser.getUsername();

        // Kiểm tra quan hệ bạn bè trước khi validate file đính kèm (người lạ nhận 403 thay vì 400)
        checkAreFriends(canonicalSender, canonicalRecipient);

        String content = (request.getContent() == null) ? "" : request.getContent().trim();
        boolean hasAttachment = fileStorageService.validateAttachment(request.getAttachmentUrl(), request.getAttachmentType());
        if (content.isEmpty() && !hasAttachment) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Nội dung tin nhắn không được để trống");
        }

        DirectMessage dm = new DirectMessage();
        dm.setSender(canonicalSender);
        dm.setRecipient(canonicalRecipient);
        dm.setContent(content);
        if (hasAttachment) {
            dm.setAttachmentUrl(request.getAttachmentUrl());
            dm.setAttachmentType(request.getAttachmentType());
        }
        dm.setRead(false);
        DirectMessage saved = directMessageRepository.save(dm);

        DirectMessageResponse response = DirectMessageResponse.from(saved);

        // Chỉ phát WebSocket sau khi transaction đã commit thành công xuống DB
        Runnable broadcastTask = () -> {
            messagingTemplate.convertAndSend("/topic/direct." + canonicalRecipient, response);
            messagingTemplate.convertAndSend("/topic/direct." + canonicalSender, response);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcastTask.run();
                }
            });
        } else {
            broadcastTask.run();
        }

        return response;
    }

    @Override
    public List<DirectMessageResponse> history(String currentUser, String friendUsername, int page, int size) {
        if (friendUsername == null || friendUsername.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tên người dùng không được để trống");
        }

        String friend = friendUsername.trim();
        if (currentUser.equalsIgnoreCase(friend)) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        checkAreFriends(currentUser, friend);

        int pageSize = Math.min(Math.max(size, 1), 100);
        List<DirectMessage> newestFirst = directMessageRepository.findBetweenUsers(
                currentUser, friend, PageRequest.of(Math.max(page, 0), pageSize));

        List<DirectMessageResponse> result = new ArrayList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            result.add(DirectMessageResponse.from(newestFirst.get(i)));
        }
        return result;
    }

    @Override
    public List<ConversationResponse> listConversations(String currentUser) {
        List<Friendship> friends = friendshipRepository.findAllAcceptedFriends(currentUser);
        List<ConversationResponse> conversations = new ArrayList<>();

        for (Friendship f : friends) {
            String friend = f.getRequester().equalsIgnoreCase(currentUser) ? f.getAddressee() : f.getRequester();
            List<DirectMessage> latestList = directMessageRepository.findBetweenUsers(currentUser, friend, PageRequest.of(0, 1));
            long unread = directMessageRepository.countBySenderAndRecipientAndIsReadFalse(friend, currentUser);

            if (!latestList.isEmpty()) {
                DirectMessage latest = latestList.get(0);
                String preview = buildPreviewContent(latest);
                conversations.add(new ConversationResponse(friend, preview, latest.getSender(), latest.getSentAt(), unread));
            } else {
                conversations.add(new ConversationResponse(friend, null, null, null, 0));
            }
        }
        return conversations;
    }

    @Override
    @Transactional
    public DirectMessageResponse markAsRead(Long messageId, String currentUser) {
        Optional<DirectMessage> found = directMessageRepository.findById(messageId);
        if (found.isEmpty()) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        DirectMessage dm = found.get();
        if (!dm.getRecipient().equalsIgnoreCase(currentUser)) {
            throw new AppException(ErrorCode.NOT_MESSAGE_RECIPIENT);
        }
        dm.setRead(true);
        DirectMessage saved = directMessageRepository.save(dm);
        return DirectMessageResponse.from(saved);
    }

    private void checkAreFriends(String user1, String user2) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findBetweenUsers(user1, user2);
        if (friendshipOpt.isEmpty() || !"ACCEPTED".equals(friendshipOpt.get().getStatus())) {
            throw new AppException(ErrorCode.NOT_FRIENDS);
        }
    }

    private String buildPreviewContent(DirectMessage message) {
        if (message.getContent() != null && !message.getContent().isBlank()) {
            return message.getContent();
        }
        String type = message.getAttachmentType();
        if (type == null) {
            return "[Tệp đính kèm]";
        }
        return switch (type.toUpperCase()) {
            case "IMAGE" -> "[Hình ảnh]";
            case "AUDIO" -> "[Ghi âm]";
            case "VIDEO" -> "[Video]";
            default -> "[Tệp đính kèm]";
        };
    }
}