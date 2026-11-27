package com.example.network.controller;

import com.example.network.entity.request.ForwardMessageRequest;
import com.example.network.entity.response.ForwardResultResponse;
import com.example.network.service.ForwardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý chuyển tiếp tin nhắn tới nhiều bạn bè / nhóm.
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ForwardController {

    private final ForwardService forwardService;

    @PostMapping("/{messageId}/forward")
    public ResponseEntity<ForwardResultResponse> forward(@PathVariable Long messageId,
                                                         @Valid @RequestBody ForwardMessageRequest request,
                                                         Authentication auth) {
        return ResponseEntity.ok(forwardService.forward(messageId, request, auth.getName()));
    }
}
