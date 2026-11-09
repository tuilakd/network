package com.example.network.controller;

import com.example.network.entity.request.DirectMessageRequest;
import com.example.network.entity.response.ConversationResponse;
import com.example.network.entity.response.DirectMessageResponse;
import com.example.network.service.DirectMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các yêu cầu nhắn tin riêng 1-1 giữa bạn bè.
 */
@Slf4j
@RestController
@RequestMapping("/api/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @PostMapping
    public ResponseEntity<DirectMessageResponse> send(@Valid @RequestBody DirectMessageRequest request,
                                                      Authentication auth) {
        log.info("Người dùng '{}' gửi tin nhắn riêng tới '{}'", auth.getName(), request.getRecipient());
        DirectMessageResponse response = directMessageService.send(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{friendUsername}")
    public ResponseEntity<List<DirectMessageResponse>> history(@PathVariable String friendUsername,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "50") int size,
                                                               Authentication auth) {
        log.info("Người dùng '{}' tải lịch sử tin nhắn với '{}' (page={}, size={})", auth.getName(), friendUsername, page, size);
        List<DirectMessageResponse> list = directMessageService.history(auth.getName(), friendUsername, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> listConversations(Authentication auth) {
        log.info("Người dùng '{}' tải danh sách các cuộc trò chuyện 1-1", auth.getName());
        List<ConversationResponse> list = directMessageService.listConversations(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<DirectMessageResponse> markAsRead(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' đánh dấu đã đọc tin nhắn id={}", auth.getName(), id);
        DirectMessageResponse response = directMessageService.markAsRead(id, auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
