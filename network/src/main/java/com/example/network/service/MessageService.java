package com.example.network.service;

import com.example.network.entity.response.ChatMessageResponse;

import java.util.List;

public interface MessageService {

    ChatMessageResponse send(Long roomId, String username, String content);

    List<ChatMessageResponse> history(Long roomId, String username, int page, int size);
}