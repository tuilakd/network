package com.example.network.service;

import com.example.network.entity.response.FriendshipResponse;

import java.util.List;

public interface FriendshipService {

    FriendshipResponse sendRequest(String requester, String targetUsername);

    List<FriendshipResponse> listFriends(String username);

    List<FriendshipResponse> listPendingRequests(String username);

    FriendshipResponse acceptRequest(Long requestId, String username);

    FriendshipResponse declineRequest(Long requestId, String username);

    void unfriend(String username, String friendUsername);
}
