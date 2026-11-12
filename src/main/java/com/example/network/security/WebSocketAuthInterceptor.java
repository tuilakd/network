package com.example.network.security;

import com.example.network.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor kiểm tra xác thực JWT khi kết nối WebSocket (CONNECT)
 * và phân quyền thành viên khi đăng ký nhận tin theo phòng (SUBSCRIBE /topic/room.{id}).
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/room\\.(\\d+)$");
    private static final Pattern DIRECT_TOPIC = Pattern.compile("^/topic/direct\\.(.+)$");
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RoomService roomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Thiếu token");
            }
            String token = header.substring(7);
            if (!jwtUtil.isValid(token)) {
                throw new MessagingException("Token không hợp lệ hoặc đã hết hạn");
            }
            UserDetails ud = userDetailsService.loadUserByUsername(jwtUtil.extractUsername(token));
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    ud.getUsername(), null, ud.getAuthorities()));

        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user == null) {
                throw new MessagingException("Chưa đăng nhập");
            }
            String dest = accessor.getDestination();
            if (dest != null && dest.startsWith("/topic/")) {
                Matcher roomMatcher = ROOM_TOPIC.matcher(dest);
                Matcher directMatcher = DIRECT_TOPIC.matcher(dest);
                if (roomMatcher.matches()) {
                    roomService.requireMember(Long.parseLong(roomMatcher.group(1)), user.getName());
                } else if (directMatcher.matches()) {
                    // Kênh tin nhắn riêng: chỉ chính chủ mới được đăng ký nhận
                    if (!directMatcher.group(1).equals(user.getName())) {
                        throw new MessagingException("Không có quyền nhận kênh tin nhắn của người khác");
                    }
                } else {
                    throw new MessagingException("Kênh không hợp lệ");
                }
            }
        }
        return message;
    }
}