package com.example.network.controller;

import com.example.network.dto.ChatMessageRequest;
import com.example.network.dto.ChatMessageResponse;
import com.example.network.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<ChatMessageResponse> history(@PathVariable Long roomId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size,
                                             Authentication auth) {
        return messageService.history(roomId, auth.getName(), page, size);
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> send(@PathVariable Long roomId,
                                                    @Valid @RequestBody ChatMessageRequest req,
                                                    Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.send(roomId, auth.getName(), req.getContent()));
    }
}