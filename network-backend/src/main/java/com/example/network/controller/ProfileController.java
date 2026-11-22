package com.example.network.controller;

import com.example.network.entity.request.ProfileUpdateRequest;
import com.example.network.entity.response.ProfileResponse;
import com.example.network.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các yêu cầu xem và cập nhật hồ sơ người dùng.
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication auth) {
        log.info("Người dùng '{}' truy vấn hồ sơ cá nhân", auth.getName());
        ProfileResponse response = profileService.getMyProfile(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                                           Authentication auth) {
        log.info("Người dùng '{}' cập nhật hồ sơ cá nhân", auth.getName());
        ProfileResponse response = profileService.updateMyProfile(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getUserProfile(@PathVariable String username, Authentication auth) {
        log.info("Người dùng '{}' xem hồ sơ công khai của '{}'", auth.getName(), username);
        ProfileResponse response = profileService.getUserProfile(username);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
