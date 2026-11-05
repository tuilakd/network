package com.example.network.controller;

import com.example.network.entity.ChatRoom;
import com.example.network.entity.request.RoomRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ChatRoom> create(@Valid @RequestBody RoomRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.create(request.getName(), auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ChatRoom>> listAll() {
        return ResponseEntity.ok(roomService.listAll());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ChatRoom>> listMine(Authentication auth) {
        return ResponseEntity.ok(roomService.listMine(auth.getName()));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiMessageResponse> join(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(roomService.join(id, auth.getName()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<String>> members(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(roomService.members(id, auth.getName()));
    }
}