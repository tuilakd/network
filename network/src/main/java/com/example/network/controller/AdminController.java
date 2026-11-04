package com.example.network.controller;

import com.example.network.entity.User;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.repository.RoomMemberRepository;
import com.example.network.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record UserSummary(Long id, String username, String role) {}

    private final UserRepository userRepository;
    private final RoomMemberRepository memberRepository;

    public AdminController(UserRepository userRepository, RoomMemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @GetMapping("/users")
    public List<UserSummary> users() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummary(u.getId(), u.getUsername(), u.getRole()))
                .toList();
    }

    @PutMapping("/users/{id}/role")
    public UserSummary changeRole(@PathVariable Long id,
                                  @RequestBody Map<String, String> body,
                                  Authentication auth) {
        String role = body.get("role");
        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role phải là USER hoặc ADMIN");
        }
        User u = find(id);
        if (u.getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự đổi quyền của chính mình");
        }
        u.setRole(role);
        userRepository.save(u);
        return new UserSummary(u.getId(), u.getUsername(), u.getRole());
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public Map<String, String> delete(@PathVariable Long id, Authentication auth) {
        User u = find(id);
        if (u.getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự xóa chính mình");
        }
        memberRepository.deleteByUsername(u.getUsername());
        userRepository.delete(u);
        return Map.of("message", "Đã xóa user " + u.getUsername());
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + id));
    }
}