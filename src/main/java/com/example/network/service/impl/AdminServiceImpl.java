package com.example.network.service.impl;

import com.example.network.entity.enums.UserRole;
import com.example.network.entity.response.UserSummary;
import com.example.network.entity.User;
import com.example.network.exception.BadRequestException;
import com.example.network.exception.NetError;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.exception.code.DomainCode;
import com.example.network.repository.*;
import com.example.network.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoomMemberRepository memberRepository;
    private final FriendshipRepository friendshipRepository;
    private final DirectMessageRepository directMessageRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DataRepository dataRepository;
    private final UserProfileRepository profileRepository;

    @Override
    public List<UserSummary> listUsers() {
        List<UserSummary> result = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            result.add(new UserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole()));
        }
        return result;
    }

    @Override
    public UserSummary changeRole(Long userId, UserRole role, String currentUsername) {
        User user = findUser(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new BadRequestException(new NetError(DomainCode.CANNOT_CHANGE_OWN_ROLE));
        }
        user.setRole(role);
        userRepository.save(user);
        return new UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Override
    @Transactional
    public String deleteUser(Long userId, String currentUsername) {
        User user = findUser(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new BadRequestException(new NetError(DomainCode.CANNOT_DELETE_SELF));
        }
        String name = user.getUsername();
        memberRepository.deleteByUsername(name);
        friendshipRepository.deleteByRequesterOrAddressee(name, name);
        directMessageRepository.deleteBySenderOrRecipient(name, name);
        chatMessageRepository.deleteBySender(name);
        dataRepository.deleteByOwner(name);
        profileRepository.deleteByUsername(name);
        userRepository.delete(user);
        return name;
    }

    private User findUser(Long id) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            log.error("Không tìm thấy user với id [{}] : " , id);
            throw new ResourceNotFoundException(new NetError(DomainCode.USER_NOT_FOUND));
        }
        return found.get();
    }
}