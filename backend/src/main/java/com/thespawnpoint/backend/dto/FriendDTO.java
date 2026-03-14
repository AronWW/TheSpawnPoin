package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class FriendDTO {
    private Long userId;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String status;
    private String lastSeen;
    private Instant friendsSince;
}

