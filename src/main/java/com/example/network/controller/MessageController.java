package com.example.network.controller;

import com.example.network.entity.request.ChatMessageRequest;
import com.example.network.entity.response.ChatMessageResponse;
import com.example.network.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý lịch sử tin nhắn và gửi tin nhắn REST API trong phòng chat.
 */
@Slf4j
@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> history(@PathVariable Long roomId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size,
                                                             Authentication auth) {
        log.info("Người dùng '{}' truy vấn lịch sử tin nhắn phòng id={}, page={}, size={}",
                auth.getName(), roomId, page, size);
        List<ChatMessageResponse> messages = messageService.history(roomId, auth.getName(), page, size);
        return ResponseEntity.status(HttpStatus.OK).body(messages);
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> send(@PathVariable Long roomId,
                                                    @Valid @RequestBody ChatMessageRequest request,
                                                    Authentication auth) {
        log.info("Người dùng '{}' gửi tin nhắn qua REST API tới phòng id={}", auth.getName(), roomId);
        ChatMessageResponse response = messageService.send(roomId, auth.getName(), request.getContent(),
                request.getAttachmentUrl(), request.getAttachmentType());
        log.info("Tin nhắn id={} đã được gửi thành công vào phòng id={}", response.getId(), roomId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}