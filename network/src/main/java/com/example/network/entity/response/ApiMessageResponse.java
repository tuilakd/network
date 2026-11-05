package com.example.network.entity.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phản hồi thông báo chung từ API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiMessageResponse {

    private String message;
}
