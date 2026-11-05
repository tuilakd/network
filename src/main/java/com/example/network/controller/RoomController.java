package com.example.network.controller;

import com.example.network.entity.ChatRoom;
import com.example.network.entity.request.RoomRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý tạo phòng, xem danh sách phòng và tham gia phòng chat.
 */
@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ChatRoom> create(@Valid @RequestBody RoomRequest request, Authentication auth) {
        log.info("Người dùng '{}' yêu cầu tạo phòng chat mới: '{}'", auth.getName(), request.getName());
        ChatRoom room = roomService.create(request.getName(), auth.getName());
        log.info("Phòng chat '{}' (id={}) được tạo thành công bởi '{}'", room.getName(), room.getId(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping
    public ResponseEntity<List<ChatRoom>> listAll() {
        log.info("Lấy danh sách tất cả các phòng chat");
        List<ChatRoom> rooms = roomService.listAll();
        return ResponseEntity.status(HttpStatus.OK).body(rooms);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ChatRoom>> listMine(Authentication auth) {
        log.info("Lấy danh sách các phòng chat mà người dùng '{}' đã tham gia", auth.getName());
        List<ChatRoom> myRooms = roomService.listMine(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(myRooms);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiMessageResponse> join(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' yêu cầu tham gia phòng id={}", auth.getName(), id);
        roomService.join(id, auth.getName());
        log.info("Người dùng '{}' đã tham gia thành công phòng id={}", auth.getName(), id);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã tham gia phòng"));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<String>> members(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' lấy danh sách thành viên của phòng id={}", auth.getName(), id);
        List<String> members = roomService.members(id, auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(members);
    }
}