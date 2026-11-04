package com.example.network.service;

import com.example.network.entity.ChatRoom;
import com.example.network.entity.RoomMember;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.repository.ChatRoomRepository;
import com.example.network.repository.RoomMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RoomService {

    private final ChatRoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;

    public RoomService(ChatRoomRepository roomRepository, RoomMemberRepository memberRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public ChatRoom create(String name, String username) {
        if (roomRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên phòng đã tồn tại");
        }
        ChatRoom room = new ChatRoom();
        room.setName(name);
        room.setCreatedBy(username);
        room = roomRepository.save(room);
        addMember(room.getId(), username);   // người tạo tự động vào phòng
        return room;
    }

    public List<ChatRoom> listAll() {
        return roomRepository.findAll();
    }

    public List<ChatRoom> listMine(String username) {
        List<Long> ids = memberRepository.findByUsername(username)
                .stream().map(RoomMember::getRoomId).toList();
        return roomRepository.findAllById(ids);
    }

    public void join(Long roomId, String username) {
        findRoom(roomId);
        if (memberRepository.existsByRoomIdAndUsername(roomId, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã ở trong phòng này");
        }
        addMember(roomId, username);
    }

    public List<String> members(Long roomId, String username) {
        findRoom(roomId);
        requireMember(roomId, username);
        return memberRepository.findByRoomId(roomId)
                .stream().map(RoomMember::getUsername).toList();
    }

    public ChatRoom findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng id=" + roomId));
    }

    public void requireMember(Long roomId, String username) {
        if (!memberRepository.existsByRoomIdAndUsername(roomId, username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa tham gia phòng này");
        }
    }

    private void addMember(Long roomId, String username) {
        RoomMember m = new RoomMember();
        m.setRoomId(roomId);
        m.setUsername(username);
        memberRepository.save(m);
    }
}