package com.example.network.service;

import com.example.network.entity.ChatRoom;

import java.util.List;

public interface RoomService {

    ChatRoom create(String name, String username);

    List<ChatRoom> listAll();

    List<ChatRoom> listMine(String username);

    void join(Long roomId, String username);

    List<String> members(Long roomId, String username);

    ChatRoom findRoom(Long roomId);

    void requireMember(Long roomId, String username);
}