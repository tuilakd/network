package com.example.network.controller;

import com.example.network.dto.ChatMessageRequest;
import com.example.network.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final MessageService messageService;

    @MessageMapping("/chat.send/{roomId}")
    public void send(@DestinationVariable Long roomId,
                     @Payload ChatMessageRequest request,
                     Principal principal) {
        messageService.send(roomId, principal.getName(), request.getContent());
    }

    // Lỗi chỉ gửi riêng cho người gửi, qua kênh /user/queue/errors
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleError(Exception e) {
        if (e instanceof ResponseStatusException) {
            ResponseStatusException r = (ResponseStatusException) e;
            if (r.getReason() != null) {
                return r.getReason();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
    }
}