package com.example.network.config;

import com.example.network.entity.User;
import com.example.network.repository.UserRepository;
import com.example.network.service.ProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           ProfileService profileService,
                           @Value("${app.admin.password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileService = profileService;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        Optional<User> found = userRepository.findByUsername("admin");
        if (found.isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);
        } else {
            User admin = found.get();
            boolean changed = false;
            if (!"ADMIN".equals(admin.getRole())) {
                admin.setRole("ADMIN");
                changed = true;
            }
            if (admin.getEmail() == null) {
                admin.setEmail("admin@example.com");
                changed = true;
            }
            if (changed) {
                userRepository.save(admin);
            }
        }
        profileService.createInitialProfile("admin");
    }
}