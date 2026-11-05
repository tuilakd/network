package com.example.network.service.impl;

import com.example.network.entity.response.UserSummary;
import com.example.network.entity.User;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.repository.RoomMemberRepository;
import com.example.network.repository.UserRepository;
import com.example.network.service.AdminService;
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
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoomMemberRepository memberRepository;

    @Override
    public List<UserSummary> listUsers() {
        List<UserSummary> result = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            result.add(new UserSummary(u.getId(), u.getUsername(), u.getRole()));
        }
        return result;
    }

    @Override
    public UserSummary changeRole(Long userId, String role, String currentUsername) {
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role phải là USER hoặc ADMIN");
        }
        User user = findUser(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự đổi quyền của chính mình");
        }
        user.setRole(role);
        userRepository.save(user);
        return new UserSummary(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    @Transactional
    public String deleteUser(Long userId, String currentUsername) {
        User user = findUser(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự xóa chính mình");
        }
        memberRepository.deleteByUsername(user.getUsername());
        userRepository.delete(user);
        return user.getUsername();
    }

    private User findUser(Long id) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy user id=" + id);
        }
        return found.get();
    }
}