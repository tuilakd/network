package com.example.network.service;

import com.example.network.dto.ChatMessageResponse;
import com.example.network.entity.ChatMessage;
import com.example.network.repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MessageService {

    private final ChatMessageRepository messageRepository;
    private final RoomService roomService;

    public MessageService(ChatMessageRepository messageRepository, RoomService roomService) {
        this.messageRepository = messageRepository;
        this.roomService = roomService;
    }

    public ChatMessageResponse send(Long roomId, String username, String content) {
        roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);

        ChatMessage m = new ChatMessage();
        m.setRoomId(roomId);
        m.setSender(username);
        m.setContent(content.trim());
        return ChatMessageResponse.from(messageRepository.save(m));
    }

    public List<ChatMessageResponse> history(Long roomId, String username, int page, int size) {
        roomService.findRoom(roomId);
        roomService.requireMember(roomId, username);

        List<ChatMessageResponse> result = new ArrayList<>(
                messageRepository
                        .findByRoomIdOrderBySentAtDesc(roomId, PageRequest.of(page, Math.min(size, 100)))
                        .stream().map(ChatMessageResponse::from).toList());
        Collections.reverse(result);   // cũ trước, mới sau
        return result;
    }
}