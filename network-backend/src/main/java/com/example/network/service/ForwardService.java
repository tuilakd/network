package com.example.network.service;

import com.example.network.entity.request.ForwardMessageRequest;
import com.example.network.entity.response.ForwardResultResponse;

public interface ForwardService {

    ForwardResultResponse forward(Long messageId, ForwardMessageRequest request, String username);
}
