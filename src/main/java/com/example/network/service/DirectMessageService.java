package com.example.network.service;

import com.example.network.entity.request.DirectMessageRequest;
import com.example.network.entity.response.ConversationResponse;
import com.example.network.entity.response.DirectMessageResponse;

import java.util.List;

public interface DirectMessageService {

    DirectMessageResponse send(String sender, DirectMessageRequest request);

    List<DirectMessageResponse> history(String currentUser, String friendUsername, int page, int size);

    List<ConversationResponse> listConversations(String currentUser);

    DirectMessageResponse markAsRead(Long messageId, String currentUser);
}
