package com.thespawnpoint.backend.security;

import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            if (cookies != null) {
                String token = Arrays.stream(cookies)
                        .filter(c -> "access_token".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);

                if (token != null && jwtUtil.isTokenValid(token)
                        && "access".equals(jwtUtil.extractType(token))) {
                    String email = jwtUtil.extractEmail(token);
                    userRepository.findByEmail(email).ifPresent(user -> {
                        if (user.isEmailVerified()) {
                            attributes.put("email", email);
                            log.debug("WebSocket handshake authorized: {}", email);
                        }
                    });
                }
            }
        }

        if (!attributes.containsKey("email")) {
            log.warn("WebSocket handshake rejected: unauthenticated");
            return false;
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}

