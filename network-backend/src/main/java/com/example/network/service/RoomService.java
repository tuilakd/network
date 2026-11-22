package com.example.network.service;

import com.example.network.entity.ChatRoom;

import java.util.List;

public interface RoomService {

    ChatRoom create(String name, String username, List<String> initialMembers);

    default ChatRoom create(String name, String username) {
        return create(name, username, null);
    }

    List<ChatRoom> listAll();

    List<ChatRoom> listMine(String username);

    void join(Long roomId, String username);

    void addMember(Long roomId, String currentUsername, String targetUsername);

    void updateName(Long roomId, String currentUsername, String newName);

    void leave(Long roomId, String currentUsername);

    void kickMember(Long roomId, String currentUsername, String targetUsername);

    List<String> members(Long roomId, String username);

    ChatRoom findRoom(Long roomId);

    void requireMember(Long roomId, String username);
}