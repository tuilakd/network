package com.example.network.service.impl;

import com.example.network.entity.DirectMessage;
import com.example.network.entity.Friendship;
import com.example.network.entity.request.DirectMessageRequest;
import com.example.network.entity.response.ConversationResponse;
import com.example.network.entity.response.DirectMessageResponse;
import com.example.network.exception.AppException;
import com.example.network.exception.ErrorCode;
import com.example.network.repository.DirectMessageRepository;
import com.example.network.repository.FriendshipRepository;
import com.example.network.repository.UserRepository;
import com.example.network.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public DirectMessageResponse send(String sender, DirectMessageRequest request) {
        String recipient = request.getRecipient().trim();
        String content = request.getContent().trim();

        if (sender.equalsIgnoreCase(recipient)) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        if (!userRepository.existsByUsername(recipient)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Bắt buộc phải là bạn bè mới được nhắn tin 1-1
        checkAreFriends(sender, recipient);

        DirectMessage dm = new DirectMessage();
        dm.setSender(sender);
        dm.setRecipient(recipient);
        dm.setContent(content);
        dm.setRead(false);
        DirectMessage saved = directMessageRepository.save(dm);

        DirectMessageResponse response = DirectMessageResponse.from(saved);

        // Phát WebSocket thời gian thực tới topic cá nhân của người nhận và người gửi
        messagingTemplate.convertAndSend("/topic/direct." + recipient, response);
        messagingTemplate.convertAndSend("/topic/direct." + sender, response);

        return response;
    }

    @Override
    public List<DirectMessageResponse> history(String currentUser, String friendUsername, int page, int size) {
        String friend = friendUsername.trim();
        if (currentUser.equalsIgnoreCase(friend)) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        checkAreFriends(currentUser, friend);

        int pageSize = Math.min(Math.max(size, 1), 100);
        List<DirectMessage> newestFirst = directMessageRepository.findBetweenUsers(
                currentUser, friend, PageRequest.of(Math.max(page, 0), pageSize));

        // Đảo ngược thứ tự: tin nhắn cũ lên trước, tin mới sau
        List<DirectMessageResponse> result = new ArrayList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            result.add(DirectMessageResponse.from(newestFirst.get(i)));
        }
        return result;
    }

    @Override
    public List<ConversationResponse> listConversations(String currentUser) {
        List<Friendship> friends = friendshipRepository.findAllAcceptedFriends(currentUser);
        List<ConversationResponse> conversations = new ArrayList<>();

        for (Friendship f : friends) {
            String friend = f.getRequester().equals(currentUser) ? f.getAddressee() : f.getRequester();
            List<DirectMessage> latestList = directMessageRepository.findBetweenUsers(currentUser, friend, PageRequest.of(0, 1));
            long unread = directMessageRepository.countBySenderAndRecipientAndIsReadFalse(friend, currentUser);

            if (!latestList.isEmpty()) {
                DirectMessage latest = latestList.get(0);
                conversations.add(new ConversationResponse(friend, latest.getContent(), latest.getSender(), latest.getSentAt(), unread));
            } else {
                conversations.add(new ConversationResponse(friend, null, null, null, 0));
            }
        }
        return conversations;
    }

    @Override
    @Transactional
    public DirectMessageResponse markAsRead(Long messageId, String currentUser) {
        Optional<DirectMessage> found = directMessageRepository.findById(messageId);
        if (found.isEmpty()) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        DirectMessage dm = found.get();
        if (!dm.getRecipient().equals(currentUser)) {
            throw new AppException(ErrorCode.NOT_MESSAGE_RECIPIENT);
        }
        dm.setRead(true);
        DirectMessage saved = directMessageRepository.save(dm);
        return DirectMessageResponse.from(saved);
    }

    private void checkAreFriends(String user1, String user2) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findBetweenUsers(user1, user2);
        if (friendshipOpt.isEmpty() || !"ACCEPTED".equals(friendshipOpt.get().getStatus())) {
            throw new AppException(ErrorCode.NOT_FRIENDS);
        }
    }
}
