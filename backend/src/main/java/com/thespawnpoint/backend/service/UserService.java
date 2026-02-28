package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.UserSummaryDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserSummaryDTO> search(String query, User currentUser) {
        return userRepository.searchByQuery(query, currentUser.getId())
                .stream()
                .map(this::toSearchDTO)
                .toList();
    }

    private UserSummaryDTO toSearchDTO(User u) {
        return UserSummaryDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .displayName(u.getDisplayName())
                .status(u.getStatus().name())
                .lastSeen(u.getLastSeen() != null ? u.getLastSeen().toString() : null)
                .lastMessage(null)
                .lastMessageAt(null)
                .unreadCount(0)
                .build();
    }
}
