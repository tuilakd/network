package com.example.network.controller;

import com.example.network.entity.request.FriendRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.entity.response.FriendshipResponse;
import com.example.network.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các nghiệp vụ kết bạn và quản lý quan hệ bạn bè.
 */
@Slf4j
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<FriendshipResponse> sendRequest(@Valid @RequestBody FriendRequest request,
                                                          Authentication auth) {
        log.info("Người dùng '{}' gửi lời mời kết bạn tới '{}'", auth.getName(), request.getTargetUsername());
        FriendshipResponse response = friendshipService.sendRequest(auth.getName(), request.getTargetUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FriendshipResponse>> listFriends(Authentication auth) {
        log.info("Lấy danh sách bạn bè của '{}'", auth.getName());
        List<FriendshipResponse> list = friendshipService.listFriends(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<FriendshipResponse>> listPending(Authentication auth) {
        log.info("Lấy danh sách lời mời kết bạn chờ duyệt của '{}'", auth.getName());
        List<FriendshipResponse> list = friendshipService.listPendingRequests(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<FriendshipResponse> acceptRequest(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' chấp nhận lời mời kết bạn id={}", auth.getName(), id);
        FriendshipResponse response = friendshipService.acceptRequest(id, auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/requests/{id}/decline")
    public ResponseEntity<FriendshipResponse> declineRequest(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' từ chối lời mời kết bạn id={}", auth.getName(), id);
        FriendshipResponse response = friendshipService.declineRequest(id, auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{friendUsername}")
    public ResponseEntity<ApiMessageResponse> unfriend(@PathVariable String friendUsername, Authentication auth) {
        log.info("Người dùng '{}' yêu cầu hủy kết bạn với '{}'", auth.getName(), friendUsername);
        friendshipService.unfriend(auth.getName(), friendUsername);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã hủy kết bạn"));
    }
}
