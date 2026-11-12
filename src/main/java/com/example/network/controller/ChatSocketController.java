package com.example.network.controller;

import com.example.network.entity.request.ChatMessageRequest;
import com.example.network.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Controller xử lý các tin nhắn WebSocket STOMP gửi từ client tới server.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final MessageService messageService;

    @MessageMapping("/chat.send/{roomId}")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatMessageRequest request,
                     Principal principal) {
        log.info("Nhận tin nhắn WebSocket từ '{}' vào phòng id={}", principal.getName(), roomId);
        messageService.send(roomId, principal.getName(), request.getContent(),
                request.getAttachmentUrl(), request.getAttachmentType());
    }

    // Lỗi chỉ gửi riêng cho người gửi, qua kênh /user/queue/errors
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleError(Exception e) {
        log.warn("Xảy ra lỗi trong xử lý WebSocket: {}", e.getMessage());
        if (e instanceof ResponseStatusException) {
            ResponseStatusException r = (ResponseStatusException) e;
            if (r.getReason() != null) {
                return r.getReason();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
    }
}