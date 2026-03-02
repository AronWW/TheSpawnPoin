package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class FriendRequestDTO {
    private Long inviteId;
    private Long senderId;
    private String senderEmail;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private Instant createdAt;
}

