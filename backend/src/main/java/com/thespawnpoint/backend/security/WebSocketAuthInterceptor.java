package com.thespawnpoint.backend.security;

import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && jwtUtil.isTokenValid(token)) {
                String type = jwtUtil.extractType(token);
                if ("access".equals(type)) {
                    String email = jwtUtil.extractEmail(token);
                    userRepository.findByEmail(email).ifPresent(user -> {
                        if (user.isEmailVerified()) {
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            email, null, Collections.emptyList());
                            auth.setDetails(user);
                            accessor.setUser(auth);
                            log.debug("WebSocket CONNECT authenticated: {}", email);
                        }
                    });
                }
            } else {
                log.warn("WebSocket CONNECT rejected: invalid or missing token");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> headerValues = accessor.getNativeHeader("access_token");
        if (headerValues != null && !headerValues.isEmpty()) {
            return headerValues.get(0);
        }

        List<String> cookies = accessor.getNativeHeader("cookie");
        if (cookies == null) return null;

        for (String cookieHeader : cookies) {
            for (String part : cookieHeader.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("access_token=")) {
                    return trimmed.substring("access_token=".length());
                }
            }
        }
        return null;
    }
}

