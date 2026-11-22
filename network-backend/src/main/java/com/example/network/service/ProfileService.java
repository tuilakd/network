package com.example.network.service;

import com.example.network.entity.request.ProfileUpdateRequest;
import com.example.network.entity.response.ProfileResponse;

public interface ProfileService {

    ProfileResponse getMyProfile(String username);

    ProfileResponse updateMyProfile(String username, ProfileUpdateRequest request);

    ProfileResponse getUserProfile(String targetUsername);

    void createInitialProfile(String username);
}
