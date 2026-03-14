package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatParticipantDTO {
    private Long userId;
    private String displayName;
    private String email;
    private String avatarUrl;
}

