package com.example.network.service.impl;

import com.example.network.entity.Friendship;
import com.example.network.entity.User;
import com.example.network.entity.response.FriendshipResponse;
import com.example.network.exception.AppException;
import com.example.network.exception.ErrorCode;
import com.example.network.repository.FriendshipRepository;
import com.example.network.repository.UserRepository;
import com.example.network.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FriendshipResponse sendRequest(String requester, String targetUsername) {
        if (targetUsername == null || targetUsername.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tên người dùng không được để trống");
        }

        String trimmedTarget = targetUsername.trim();
        if (requester.equalsIgnoreCase(trimmedTarget)) {
            throw new AppException(ErrorCode.CANNOT_ADD_SELF);
        }

        // Lấy entity User từ DB để lấy username chuẩn xác (đúng chữ hoa/thường)
        User targetUser = userRepository.findByUsername(trimmedTarget)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng '" + trimmedTarget + "'"));

        String canonicalRequester = userRepository.findByUsername(requester)
                .map(User::getUsername)
                .orElse(requester);
        String canonicalTarget = targetUser.getUsername();

        Optional<Friendship> existingOpt = friendshipRepository.findBetweenUsers(canonicalRequester, canonicalTarget);
        if (existingOpt.isPresent()) {
            Friendship existing = existingOpt.get();
            if ("ACCEPTED".equals(existing.getStatus())) {
                throw new AppException(ErrorCode.ALREADY_FRIENDS);
            }
            if ("PENDING".equals(existing.getStatus())) {
                if (existing.getRequester().equalsIgnoreCase(canonicalRequester)) {
                    throw new AppException(ErrorCode.FRIEND_REQUEST_PENDING, "Bạn đã gửi lời mời kết bạn trước đó rồi");
                } else {
                    throw new AppException(ErrorCode.FRIEND_REQUEST_PENDING, "Người dùng này đã gửi lời mời cho bạn, hãy chấp nhận lời mời");
                }
            }
            // Nếu đã từng từ chối (DECLINED), cho phép gửi lại lời mời với username chuẩn
            existing.setRequester(canonicalRequester);
            existing.setAddressee(canonicalTarget);
            existing.setStatus("PENDING");
            Friendship saved = friendshipRepository.save(existing);
            return mapToResponse(saved, canonicalTarget);
        }

        Friendship f = new Friendship();
        f.setRequester(canonicalRequester);
        f.setAddressee(canonicalTarget);
        f.setStatus("PENDING");
        Friendship saved = friendshipRepository.save(f);
        return mapToResponse(saved, canonicalTarget);
    }

    @Override
    public List<FriendshipResponse> listFriends(String username) {
        List<Friendship> list = friendshipRepository.findAllAcceptedFriends(username);
        List<FriendshipResponse> result = new ArrayList<>();
        for (Friendship f : list) {
            String friend = f.getRequester().equalsIgnoreCase(username) ? f.getAddressee() : f.getRequester();
            result.add(mapToResponse(f, friend));
        }
        return result;
    }

    @Override
    public List<FriendshipResponse> listPendingRequests(String username) {
        List<Friendship> list = friendshipRepository.findByAddresseeAndStatus(username, "PENDING");
        List<FriendshipResponse> result = new ArrayList<>();
        for (Friendship f : list) {
            result.add(mapToResponse(f, f.getRequester()));
        }
        return result;
    }

    @Override
    @Transactional
    public FriendshipResponse acceptRequest(Long requestId, String username) {
        Friendship f = findRequest(requestId);
        if (!f.getAddressee().equalsIgnoreCase(username)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xử lý lời mời này");
        }
        if (!"PENDING".equals(f.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Lời mời kết bạn không ở trạng thái chờ duyệt");
        }
        f.setStatus("ACCEPTED");
        Friendship saved = friendshipRepository.save(f);
        return mapToResponse(saved, f.getRequester());
    }

    @Override
    @Transactional
    public FriendshipResponse declineRequest(Long requestId, String username) {
        Friendship f = findRequest(requestId);
        if (!f.getAddressee().equalsIgnoreCase(username)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xử lý lời mời này");
        }
        if (!"PENDING".equals(f.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Lời mời kết bạn không ở trạng thái chờ duyệt");
        }
        f.setStatus("DECLINED");
        Friendship saved = friendshipRepository.save(f);
        return mapToResponse(saved, f.getRequester());
    }

    @Override
    @Transactional
    public void unfriend(String username, String friendUsername) {
        if (friendUsername == null || friendUsername.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tên người dùng không được để trống");
        }
        Optional<Friendship> found = friendshipRepository.findBetweenUsers(username, friendUsername.trim());
        if (found.isEmpty() || !"ACCEPTED".equals(found.get().getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Hai người hiện không phải là bạn bè");
        }
        friendshipRepository.delete(found.get());
    }

    private Friendship findRequest(Long id) {
        Optional<Friendship> found = friendshipRepository.findById(id);
        if (found.isEmpty()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy lời mời kết bạn id=" + id);
        }
        return found.get();
    }

    private FriendshipResponse mapToResponse(Friendship f, String friendUsername) {
        return new FriendshipResponse(
                f.getId(),
                f.getRequester(),
                f.getAddressee(),
                f.getStatus(),
                friendUsername,
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}