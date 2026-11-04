package com.example.network.service.impl;

import com.example.network.entity.response.ChatMessageResponse;
import com.example.network.entity.ChatMessage;
import com.example.network.repository.ChatMessageRepository;
import com.example.network.service.MessageService;
import com.example.network.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ChatMessageRepository messageRepository;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public ChatMessageResponse send(Long roomId, String username, String content) {
        String text = (content == null) ? "" : content.trim();
        if (text.isEmpty() || text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung tin nhắn không hợp lệ");
        }
        roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);

        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSender(username);
        message.setContent(text);
        ChatMessageResponse response = ChatMessageResponse.from(messageRepository.save(message));

        // Phát cho mọi người đang mở phòng (WebSocket)
        messagingTemplate.convertAndSend("/topic/room." + roomId, response);
        return response;
    }

    @Override
    public List<ChatMessageResponse> history(Long roomId, String username, int page, int size) {
        roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);

        int pageSize = Math.min(Math.max(size, 1), 100);
        List<ChatMessage> newestFirst = messageRepository.findByRoomIdOrderBySentAtDesc(
                roomId, PageRequest.of(Math.max(page, 0), pageSize));

        // Đảo lại: tin cũ trước, tin mới sau
        List<ChatMessageResponse> result = new ArrayList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            result.add(ChatMessageResponse.from(newestFirst.get(i)));
        }
        return result;
    }
}