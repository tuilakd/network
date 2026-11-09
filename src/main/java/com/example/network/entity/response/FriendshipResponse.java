package com.example.network.entity.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin mối quan hệ bạn bè hoặc lời mời.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {

    private Long id;
    private String requester;
    private String addressee;
    private String status;
    private String friendUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
