package com.example.network.controller;

import com.example.network.entity.ChatRoom;
import com.example.network.entity.request.AddMemberRequest;
import com.example.network.entity.request.RoomRequest;
import com.example.network.entity.request.UpdateRoomNameRequest;
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
 * Controller xử lý tạo phòng, xem danh sách phòng, thành viên, đổi tên, rời phòng và kick thành viên.
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
        ChatRoom room = roomService.create(request.getName(), auth.getName(), request.getInitialMembers());
        log.info("Phòng chat '{}' (id={}) được tạo thành công bởi '{}'", room.getName(), room.getId(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping
    public ResponseEntity<List<ChatRoom>> listAll(Authentication auth) {
        // Chỉ người trong nhóm mới thấy nhóm của mình
        log.info("Người dùng '{}' lấy danh sách các phòng chat của mình", auth.getName());
        List<ChatRoom> rooms = roomService.listMine(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(rooms);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ChatRoom>> listMine(Authentication auth) {
        log.info("Lấy danh sách các phòng chat mà người dùng '{}' đã tham gia", auth.getName());
        List<ChatRoom> myRooms = roomService.listMine(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(myRooms);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiMessageResponse> addMember(@PathVariable Long id,
                                                        @Valid @RequestBody AddMemberRequest request,
                                                        Authentication auth) {
        log.info("Người dùng '{}' thêm '{}' vào phòng id={}", auth.getName(), request.getUsername(), id);
        roomService.addMember(id, auth.getName(), request.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã thêm thành viên vào nhóm"));
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<ApiMessageResponse> updateName(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateRoomNameRequest request,
                                                         Authentication auth) {
        log.info("Người dùng '{}' đổi tên phòng id={} thành '{}'", auth.getName(), id, request.getName());
        roomService.updateName(id, auth.getName(), request.getName());
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã đổi tên nhóm thành công"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiMessageResponse> leave(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' yêu cầu rời phòng id={}", auth.getName(), id);
        roomService.leave(id, auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã rời phòng"));
    }

    @DeleteMapping("/{id}/members/{username}")
    public ResponseEntity<ApiMessageResponse> kickMember(@PathVariable Long id,
                                                         @PathVariable String username,
                                                         Authentication auth) {
        log.info("Trưởng nhóm '{}' yêu cầu xóa '{}' khỏi phòng id={}", auth.getName(), username, id);
        roomService.kickMember(id, auth.getName(), username);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã xóa thành viên khỏi nhóm"));
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