package com.example.network.controller;

import com.example.network.entity.request.ChatMessageRequest;
import com.example.network.entity.response.ChatMessageResponse;
import com.example.network.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return ResponseEntity.ok(messageService.history(roomId, auth.getName(), page, size));
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> send(@PathVariable Long roomId,
                                                    @Valid @RequestBody ChatMessageRequest request,
                                                    Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.send(roomId, auth.getName(), request.getContent()));
    }
}