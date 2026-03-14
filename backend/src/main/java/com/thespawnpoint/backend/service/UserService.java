package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.UserSummaryDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public List<UserSummaryDTO> search(String query, User currentUser) {
        return userRepository.searchByQuery(query, currentUser.getId())
                .stream()
                .map(this::toSearchDTO)
                .toList();
    }

    private UserSummaryDTO toSearchDTO(User u) {
        String avatarUrl = profileRepository.findByUserId(u.getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        return UserSummaryDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .displayName(u.getDisplayName())
                .avatarUrl(avatarUrl)
                .status(u.getStatus().name())
                .lastSeen(u.getLastSeen() != null ? u.getLastSeen().toString() : null)
                .lastMessage(null)
                .lastMessageAt(null)
                .unreadCount(0)
                .build();
    }
}
