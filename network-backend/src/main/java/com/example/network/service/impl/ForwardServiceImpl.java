package com.example.network.service.impl;

import com.example.network.entity.ChatMessage;
import com.example.network.entity.DirectMessage;
import com.example.network.entity.request.ForwardMessageRequest;
import com.example.network.entity.response.ForwardResultResponse;
import com.example.network.entity.response.ForwardResultResponse.TargetResult;
import com.example.network.exception.AppException;
import com.example.network.exception.code.DomainCode;
import com.example.network.repository.ChatMessageRepository;
import com.example.network.repository.ChatRoomRepository;
import com.example.network.repository.DirectMessageRepository;
import com.example.network.repository.RoomMemberRepository;
import com.example.network.service.ForwardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForwardServiceImpl implements ForwardService {

    private static final int MAX_RECIPIENTS = 10;

    private final DirectMessageRepository directMessageRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ForwardTargetService forwardTargetService;

    @Override
    public ForwardResultResponse forward(Long messageId, ForwardMessageRequest request, String username) {
        List<String> friends = request.getFriendUsernames() == null ? List.of() : request.getFriendUsernames();
        List<Long> rooms = request.getRoomIds() == null ? List.of() : request.getRoomIds();
        int total = friends.size() + rooms.size();
        if (total == 0) {
            throw new AppException(DomainCode.INVALID_INPUT, "Vui lòng chọn ít nhất 1 nơi nhận");
        }
        if (total > MAX_RECIPIENTS) {
            throw new AppException(DomainCode.INVALID_INPUT, "Tối đa 10 nơi nhận mỗi lần chuyển tiếp");
        }

        String content;
        String attUrl;
        String attType;
        String type = request.getSourceType().trim().toUpperCase();

        if ("DM".equals(type)) {
            DirectMessage dm = directMessageRepository.findById(messageId)
                    .orElseThrow(() -> new AppException(DomainCode.MESSAGE_NOT_FOUND));
            if (!dm.getSender().equalsIgnoreCase(username) && !dm.getRecipient().equalsIgnoreCase(username)) {
                throw new AppException(DomainCode.FORBIDDEN, "Bạn không thuộc cuộc trò chuyện chứa tin nhắn này");
            }
            if (dm.isRecalled()) {
                throw new AppException(DomainCode.MESSAGE_ALREADY_RECALLED, "Không thể chuyển tiếp tin nhắn đã thu hồi");
            }
            content = dm.getContent();
            attUrl = dm.getAttachmentUrl();
            attType = dm.getAttachmentType();
        } else if ("ROOM".equals(type)) {
            ChatMessage cm = chatMessageRepository.findById(messageId)
                    .orElseThrow(() -> new AppException(DomainCode.MESSAGE_NOT_FOUND));
            if (!roomMemberRepository.existsByRoomIdAndUsername(cm.getRoomId(), username)) {
                throw new AppException(DomainCode.FORBIDDEN, "Bạn không phải thành viên của nhóm chứa tin nhắn này");
            }
            if (cm.isRecalled()) {
                throw new AppException(DomainCode.MESSAGE_ALREADY_RECALLED, "Không thể chuyển tiếp tin nhắn đã thu hồi");
            }
            content = cm.getContent();
            attUrl = cm.getAttachmentUrl();
            attType = cm.getAttachmentType();
        } else {
            throw new AppException(DomainCode.INVALID_INPUT, "sourceType phải là DM hoặc ROOM");
        }

        List<TargetResult> results = new ArrayList<>();
        int ok = 0;
        int fail = 0;

        for (String target : friends) {
            try {
                forwardTargetService.forwardToFriend(username, messageId, content, attUrl, attType, target);
                results.add(new TargetResult(target, "FRIEND", true, null));
                ok++;
            } catch (Exception e) {
                results.add(new TargetResult(target, "FRIEND", false, e.getMessage()));
                fail++;
            }
        }

        for (Long roomId : rooms) {
            String label = chatRoomRepository.findById(roomId).map(r -> r.getName()).orElse("Nhóm #" + roomId);
            try {
                forwardTargetService.forwardToRoom(username, messageId, content, attUrl, attType, roomId);
                results.add(new TargetResult(label, "ROOM", true, null));
                ok++;
            } catch (Exception e) {
                results.add(new TargetResult(label, "ROOM", false, e.getMessage()));
                fail++;
            }
        }

        return new ForwardResultResponse(total, ok, fail, results);
    }
}
