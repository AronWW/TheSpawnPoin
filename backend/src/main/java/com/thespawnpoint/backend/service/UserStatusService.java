package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void setOnline(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setStatus(User.Status.ONLINE);
            userRepository.save(user);
            broadcastStatus(user.getEmail(), User.Status.ONLINE, null);
            log.debug("User online: {}", email);
        });
    }

    @Transactional
    public void setOffline(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            Instant now = Instant.now();
            user.setStatus(User.Status.OFFLINE);
            user.setLastSeen(now);
            userRepository.save(user);
            broadcastStatus(user.getEmail(), User.Status.OFFLINE, now.toString());
            log.debug("User offline: {}", email);
        });
    }

    private void broadcastStatus(String email, User.Status status, String lastSeen) {
        messagingTemplate.convertAndSend("/topic/status", Optional.of(Map.of(
                "email", email,
                "status", status.name(),
                "lastSeen", lastSeen != null ? lastSeen : ""
        )));
    }
}

