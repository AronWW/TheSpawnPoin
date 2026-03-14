package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminUserDTO {
    private Long id;
    private String displayName;
    private String email;
    private String role;
    private String status;
    private boolean emailVerified;
    private boolean banned;
    private String banReason;
    private Instant bannedAt;
    private Instant lastSeen;
    private Instant createdAt;
    private String avatarUrl;
}

