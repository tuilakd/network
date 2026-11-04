package com.example.network.service.impl;

import com.example.network.entity.ChatRoom;
import com.example.network.entity.RoomMember;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.repository.ChatRoomRepository;
import com.example.network.repository.RoomMemberRepository;
import com.example.network.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final ChatRoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;

    @Override
    @Transactional
    public ChatRoom create(String name, String username) {
        String roomName = name.trim();
        if (roomRepository.existsByName(roomName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên phòng đã tồn tại");
        }
        ChatRoom room = new ChatRoom();
        room.setName(roomName);
        room.setCreatedBy(username);
        ChatRoom saved = roomRepository.save(room);
        addMember(saved.getId(), username);   // người tạo tự động vào phòng
        return saved;
    }

    @Override
    public List<ChatRoom> listAll() {
        return roomRepository.findAll();
    }

    @Override
    public List<ChatRoom> listMine(String username) {
        List<Long> roomIds = new ArrayList<>();
        for (RoomMember m : memberRepository.findByUsername(username)) {
            roomIds.add(m.getRoomId());
        }
        return roomRepository.findAllById(roomIds);
    }

    @Override
    public void join(Long roomId, String username) {
        findRoom(roomId);
        if (memberRepository.existsByRoomIdAndUsername(roomId, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã ở trong phòng này");
        }
        addMember(roomId, username);
    }

    @Override
    public List<String> members(Long roomId, String username) {
        findRoom(roomId);
        requireMember(roomId, username);
        List<String> names = new ArrayList<>();
        for (RoomMember m : memberRepository.findByRoomId(roomId)) {
            names.add(m.getUsername());
        }
        return names;
    }

    @Override
    public ChatRoom findRoom(Long roomId) {
        Optional<ChatRoom> found = roomRepository.findById(roomId);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy phòng id=" + roomId);
        }
        return found.get();
    }

    @Override
    public void requireMember(Long roomId, String username) {
        if (!memberRepository.existsByRoomIdAndUsername(roomId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa tham gia phòng này");
        }
    }

    private void addMember(Long roomId, String username) {
        RoomMember member = new RoomMember();
        member.setRoomId(roomId);
        member.setUsername(username);
        memberRepository.save(member);
    }
}