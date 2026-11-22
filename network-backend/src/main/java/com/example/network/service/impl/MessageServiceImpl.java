package com.example.network.service.impl;

import com.example.network.entity.ChatMessage;
import com.example.network.entity.ChatMessageDeletion;
import com.example.network.entity.ChatRoom;
import com.example.network.entity.response.ChatMessageResponse;
import com.example.network.exception.AppException;
import com.example.network.exception.code.DomainCode;
import com.example.network.repository.ChatMessageDeletionRepository;
import com.example.network.repository.ChatMessageRepository;
import com.example.network.service.FileStorageService;
import com.example.network.service.MessageService;
import com.example.network.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ChatMessageRepository messageRepository;
    private final ChatMessageDeletionRepository chatMessageDeletionRepository;
    private final RoomService roomService;
    private final FileStorageService fileStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatMessageResponse send(Long roomId, String username, String content,
                                    String attachmentUrl, String attachmentType) {
        String text = (content == null) ? "" : content.trim();
        roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);
        boolean hasAttachment = fileStorageService.validateAttachment(attachmentUrl, attachmentType);
        if ((text.isEmpty() && !hasAttachment) || text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung tin nhắn không hợp lệ");
        }

        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSender(username);
        message.setContent(text);
        if (hasAttachment) {
            message.setAttachmentUrl(attachmentUrl);
            message.setAttachmentType(attachmentType);
        }
        ChatMessage saved = messageRepository.save(message);
        ChatMessageResponse response = ChatMessageResponse.from(saved);

        // Phát cho mọi người đang mở phòng (WebSocket)
        messagingTemplate.convertAndSend("/topic/room." + roomId, response);
        return response;
    }

    @Override
    public List<ChatMessageResponse> history(Long roomId, String username, int page, int size) {
        roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);

        int pageSize = Math.min(Math.max(size, 1), 100);
        // Lọc bỏ những tin nhắn đã bị người dùng này xóa ở phía họ
        List<ChatMessage> newestFirst = messageRepository.findByRoomIdVisibleForUser(
                roomId, username, PageRequest.of(Math.max(page, 0), pageSize));

        // Đảo lại: tin cũ trước, tin mới sau
        List<ChatMessageResponse> result = new ArrayList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            result.add(ChatMessageResponse.from(newestFirst.get(i)));
        }
        return result;
    }

    @Override
    @Transactional
    public ChatMessageResponse recall(Long roomId, Long messageId, String username) {
        ChatRoom room = roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);

        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(DomainCode.MESSAGE_NOT_FOUND));

        if (!message.getRoomId().equals(roomId)) {
            throw new AppException(DomainCode.MESSAGE_NOT_FOUND);
        }

        // Chỉ người gửi hoặc trưởng nhóm mới có quyền thu hồi tin nhắn
        if (!message.getSender().equalsIgnoreCase(username) && !room.getCreatedBy().equalsIgnoreCase(username)) {
            throw new AppException(DomainCode.NOT_MESSAGE_SENDER);
        }

        if (message.isRecalled()) {
            throw new AppException(DomainCode.MESSAGE_ALREADY_RECALLED);
        }

        message.setRecalled(true);
        message.setRecalledAt(LocalDateTime.now());
        ChatMessage saved = messageRepository.save(message);
        ChatMessageResponse response = ChatMessageResponse.from(saved);

        // Bắn WebSocket thông báo tin nhắn đã thu hồi tới cả phòng
        messagingTemplate.convertAndSend("/topic/room." + roomId, response);
        return response;
    }

    @Override
    @Transactional
    public void deleteForMe(Long roomId, Long messageId, String username) {
        roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);

        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(DomainCode.MESSAGE_NOT_FOUND));

        if (!message.getRoomId().equals(roomId)) {
            throw new AppException(DomainCode.MESSAGE_NOT_FOUND);
        }

        if (!chatMessageDeletionRepository.existsByMessageIdAndUsername(messageId, username)) {
            chatMessageDeletionRepository.save(new ChatMessageDeletion(messageId, username));
        }
    }
}