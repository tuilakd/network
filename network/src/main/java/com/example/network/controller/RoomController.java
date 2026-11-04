package com.example.network.controller;

import com.example.network.dto.RoomRequest;
import com.example.network.entity.ChatRoom;
import com.example.network.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<ChatRoom> create(@Valid @RequestBody RoomRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.create(req.getName().trim(), auth.getName()));
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
        return Map.of("message", "Đã tham gia phòng");
    }

    @GetMapping("/{id}/members")
    public List<String> members(@PathVariable Long id, Authentication auth) {
        return roomService.members(id, auth.getName());
    }
}