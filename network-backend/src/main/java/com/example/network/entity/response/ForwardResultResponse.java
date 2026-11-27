package com.example.network.entity.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Kết quả chuyển tiếp tin nhắn cho từng nơi nhận.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForwardResultResponse {

    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<TargetResult> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetResult {
        private String target;
        private String type; // FRIEND hoặc ROOM
        private boolean success;
        private String error;
    }
}
