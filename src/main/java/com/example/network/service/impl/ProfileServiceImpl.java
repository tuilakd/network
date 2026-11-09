package com.example.network.service.impl;

import com.example.network.entity.User;
import com.example.network.entity.UserProfile;
import com.example.network.entity.request.ProfileUpdateRequest;
import com.example.network.entity.response.ProfileResponse;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.repository.UserProfileRepository;
import com.example.network.repository.UserRepository;
import com.example.network.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    public ProfileResponse getMyProfile(String username) {
        return getUserProfile(username);
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(String username, ProfileUpdateRequest request) {
        User user = findUser(username);
        UserProfile profile = findOrCreateProfile(user.getUsername());

        profile.setFullName(request.getFullName());
        profile.setBio(request.getBio());
        profile.setAvatarUrl(request.getAvatarUrl());

        UserProfile saved = profileRepository.save(profile);
        return mapToResponse(saved, user.getEmail());
    }

    @Override
    public ProfileResponse getUserProfile(String targetUsername) {
        User user = findUser(targetUsername);
        UserProfile profile = findOrCreateProfile(user.getUsername());
        return mapToResponse(profile, user.getEmail());
    }

    @Override
    @Transactional
    public void createInitialProfile(String username) {
        if (!profileRepository.existsByUsername(username)) {
            UserProfile profile = new UserProfile();
            profile.setUsername(username);
            profileRepository.save(profile);
        }
    }

    private User findUser(String username) {
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng '" + username + "'");
        }
        return found.get();
    }

    private UserProfile findOrCreateProfile(String username) {
        Optional<UserProfile> found = profileRepository.findByUsername(username);
        if (found.isPresent()) {
            return found.get();
        }
        UserProfile newProfile = new UserProfile();
        newProfile.setUsername(username);
        return profileRepository.save(newProfile);
    }

    private ProfileResponse mapToResponse(UserProfile p, String email) {
        return new ProfileResponse(
                p.getId(),
                p.getUsername(),
                email,
                p.getFullName(),
                p.getBio(),
                p.getAvatarUrl(),
                p.getUpdatedAt()
        );
    }
}
