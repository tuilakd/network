package com.example.network.entity.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin hồ sơ người dùng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String avatarUrl;
    private LocalDateTime updatedAt;
}
