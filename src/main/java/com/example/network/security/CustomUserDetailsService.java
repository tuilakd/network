package com.example.network.security;

import com.example.network.entity.User;
import com.example.network.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service tải thông tin chi tiết người dùng phục vụ Spring Security xác thực.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Sử dụng Java thuần: kiểm tra Optional bằng isEmpty() thay vì lambda orElseThrow
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            throw new UsernameNotFoundException("Không tìm thấy user: " + username);
        }
        User u = found.get();
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole())
                .build();
    }
}