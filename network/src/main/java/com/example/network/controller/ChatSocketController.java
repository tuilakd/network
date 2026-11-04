package com.example.network.controller;

import com.example.network.dto.ChatMessageRequest;
import com.example.network.dto.ChatMessageResponse;
import com.example.network.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class ChatSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate template;

    public ChatSocketController(MessageService messageService, SimpMessagingTemplate template) {
        this.messageService = messageService;
        this.template = template;
    }

    @MessageMapping("/chat.send/{roomId}")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatMessageRequest req,
                     Principal principal) {
        String content = req.getContent() == null ? "" : req.getContent().trim();
        if (content.isEmpty() || content.length() > 2000) {
            throw new IllegalArgumentException("Nội dung tin nhắn không hợp lệ");
        }
        ChatMessageResponse saved = messageService.send(roomId, principal.getName(), content);
        template.convertAndSend("/topic/room." + roomId, saved);
    }

    // Lỗi chỉ gửi riêng cho người gửi, qua kênh /user/queue/errors
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleError(Exception e) {
        if (e instanceof ResponseStatusException r && r.getReason() != null) {
            return r.getReason();
        }
        return e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
    }
}