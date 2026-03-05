package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryDTO {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String status;
    private String lastSeen;
    private String lastMessage;
    private String lastMessageAt;
    private int unreadCount;
}

