package com.example.network.service.impl;

import com.example.network.entity.ChatRoom;
import com.example.network.entity.Friendship;
import com.example.network.entity.RoomMember;
import com.example.network.entity.enums.FriendShipStatus;
import com.example.network.exception.AppException;
import com.example.network.exception.ConflictException;
import com.example.network.exception.ForbiddenException;
import com.example.network.exception.NetError;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.exception.code.DomainCode;
import com.example.network.repository.*;
import com.example.network.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final ChatRoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatRoom create(String name, String username, List<String> initialMembers) {
        String roomName = name.trim();
        ChatRoom room = new ChatRoom();
        room.setName(roomName);
        room.setCreatedBy(username);
        ChatRoom saved = roomRepository.save(room);

        // Người tạo tự động vào phòng
        addMemberToRoom(saved.getId(), username);

        // Thêm bạn bè ban đầu nếu có truyền danh sách
        if (initialMembers != null && !initialMembers.isEmpty()) {
            for (String target : initialMembers) {
                if (target == null || target.isBlank()) {
                    continue;
                }
                String targetTrimmed = target.trim();
                if (targetTrimmed.equalsIgnoreCase(username)) {
                    continue;
                }
                // Chỉ thêm nếu user tồn tại và là bạn bè với người tạo
                if (userRepository.existsByUsername(targetTrimmed)) {
                    Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(username, targetTrimmed);
                    if (friendship.isPresent() && friendship.get().getStatus() == FriendShipStatus.ACCEPTED) {
                        addMemberToRoom(saved.getId(), targetTrimmed);
                    }
                }
            }
        }
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
        if (roomIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roomRepository.findAllById(roomIds);
    }

    @Override
    @Transactional
    public void join(Long roomId, String username) {
        findRoom(roomId);
        if (memberRepository.existsByRoomIdAndUsername(roomId, username)) {
            throw new ConflictException(new NetError(DomainCode.ALREADY_ROOM_MEMBER));
        }
        addMemberToRoom(roomId, username);
    }

    @Override
    @Transactional
    public void addMember(Long roomId, String currentUsername, String targetUsername) {
        ChatRoom room = findRoom(roomId);
        requireMember(roomId, currentUsername);

        String target = targetUsername.trim();
        if (!userRepository.existsByUsername(target)) {
            throw new AppException(DomainCode.USER_NOT_FOUND);
        }
        if (memberRepository.existsByRoomIdAndUsername(roomId, target)) {
            throw new ConflictException(new NetError(DomainCode.ALREADY_ROOM_MEMBER));
        }

        // Kiểm tra điều kiện: target phải là bạn bè với ít nhất 1 thành viên hiện có trong phòng
        List<String> currentMembers = members(roomId, currentUsername);
        boolean isFriendWithAnyone = friendshipRepository.isFriendWithAnyMember(target, currentMembers);
        if (!isFriendWithAnyone) {
            throw new AppException(DomainCode.NOT_FRIEND_WITH_ANY_MEMBER);
        }

        addMemberToRoom(roomId, target);

        // Broadcast realtime
        Map<String, Object> roomEvent = Map.of(
                "type", "MEMBER_ADDED",
                "roomId", roomId,
                "operator", currentUsername,
                "target", target
        );
        sendWs("/topic/room." + roomId, roomEvent);

        Map<String, Object> userEvent = Map.of(
                "type", "ROOM_JOINED",
                "roomId", roomId,
                "roomName", room.getName(),
                "addedBy", currentUsername
        );
        sendWs("/topic/direct." + target, userEvent);
    }

    @Override
    @Transactional
    public void updateName(Long roomId, String currentUsername, String newName) {
        ChatRoom room = findRoom(roomId);
        requireMember(roomId, currentUsername);

        String trimmed = newName.trim();
        room.setName(trimmed);
        roomRepository.save(room);

        Map<String, Object> event = Map.of(
                "type", "ROOM_NAME_CHANGED",
                "roomId", roomId,
                "operator", currentUsername,
                "name", trimmed
        );
        sendWs("/topic/room." + roomId, event);
    }

    @Override
    @Transactional
    public void leave(Long roomId, String currentUsername) {
        ChatRoom room = findRoom(roomId);
        requireMember(roomId, currentUsername);

        memberRepository.deleteByRoomIdAndUsername(roomId, currentUsername);
        List<RoomMember> remaining = memberRepository.findByRoomId(roomId);

        if (remaining.isEmpty()) {
            // Không còn ai trong phòng: dọn dẹp tin nhắn và xóa phòng
            messageRepository.deleteByRoomId(roomId);
            roomRepository.delete(room);
            log.info("Phòng chat id={} đã được dọn dẹp do không còn thành viên", roomId);
        } else if (currentUsername.equalsIgnoreCase(room.getCreatedBy())) {
            // Trưởng nhóm rời đi: chuyển quyền cho thành viên tiếp theo
            String newCreator = remaining.get(0).getUsername();
            room.setCreatedBy(newCreator);
            roomRepository.save(room);

            Map<String, Object> event = Map.of(
                    "type", "MEMBER_LEFT",
                    "roomId", roomId,
                    "username", currentUsername,
                    "newCreator", newCreator
            );
            sendWs("/topic/room." + roomId, event);
        } else {
            Map<String, Object> event = Map.of(
                    "type", "MEMBER_LEFT",
                    "roomId", roomId,
                    "username", currentUsername
            );
            sendWs("/topic/room." + roomId, event);
        }
    }

    @Override
    @Transactional
    public void kickMember(Long roomId, String currentUsername, String targetUsername) {
        ChatRoom room = findRoom(roomId);
        if (!room.getCreatedBy().equalsIgnoreCase(currentUsername)) {
            throw new AppException(DomainCode.NOT_ROOM_CREATOR);
        }
        if (targetUsername.equalsIgnoreCase(room.getCreatedBy())) {
            throw new AppException(DomainCode.CANNOT_KICK_CREATOR);
        }
        if (!memberRepository.existsByRoomIdAndUsername(roomId, targetUsername)) {
            throw new AppException(DomainCode.NOT_ROOM_MEMBER);
        }

        memberRepository.deleteByRoomIdAndUsername(roomId, targetUsername);

        Map<String, Object> event = Map.of(
                "type", "MEMBER_KICKED",
                "roomId", roomId,
                "operator", currentUsername,
                "target", targetUsername
        );
        sendWs("/topic/room." + roomId, event);

        Map<String, Object> userEvent = Map.of(
                "type", "REMOVED_FROM_ROOM",
                "roomId", roomId
        );
        sendWs("/topic/direct." + targetUsername, userEvent);
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
            throw new ForbiddenException(new NetError(DomainCode.NOT_ROOM_MEMBER));
        }
    }

    private void addMemberToRoom(Long roomId, String username) {
        RoomMember member = new RoomMember();
        member.setRoomId(roomId);
        member.setUsername(username);
        memberRepository.save(member);
    }

    private void sendWs(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }
}