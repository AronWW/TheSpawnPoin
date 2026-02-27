package com.thespawnpoint.backend.config;

import com.thespawnpoint.backend.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserStatusService userStatusService;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String email = extractEmail(accessor);
        if (email != null) {
            userStatusService.setOnline(email);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String email = extractEmail(accessor);
        if (email != null) {
            userStatusService.setOffline(email);
        }
    }

    private String extractEmail(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) return null;

        if (user instanceof UsernamePasswordAuthenticationToken auth) {
            Object principal = auth.getPrincipal();
            if (principal instanceof String email) return email;
        }

        String name = user.getName();
        return (name != null && !name.isBlank()) ? name : null;
    }
}

