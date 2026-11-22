package com.example.network.service;

import com.example.network.entity.response.ChatMessageResponse;

import java.util.List;

public interface MessageService {

    ChatMessageResponse send(Long roomId, String username, String content,
                             String attachmentUrl, String attachmentType);

    List<ChatMessageResponse> history(Long roomId, String username, int page, int size);

    ChatMessageResponse recall(Long roomId, Long messageId, String username);

    void deleteForMe(Long roomId, Long messageId, String username);
}