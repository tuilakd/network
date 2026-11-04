package com.example.network.controller;

import com.example.network.entity.request.RoomRequest;
import com.example.network.entity.ChatRoom;
import com.example.network.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<ChatRoom> listAll() {
        return roomService.listAll();
    }

    @GetMapping("/mine")
    public List<ChatRoom> listMine(Authentication auth) {
        return roomService.listMine(auth.getName());
    }

    @PostMapping("/{id}/join")
    public Map<String, String> join(@PathVariable Long id, Authentication auth) {
        roomService.join(id, auth.getName());
        Map<String, String> body = new HashMap<>();
        body.put("message", "Đã tham gia phòng");
        return body;
    }

    @GetMapping("/{id}/members")
    public List<String> members(@PathVariable Long id, Authentication auth) {
        return roomService.members(id, auth.getName());
    }
}