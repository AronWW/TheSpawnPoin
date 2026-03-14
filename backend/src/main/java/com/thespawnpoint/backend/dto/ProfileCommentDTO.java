package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProfileCommentDTO {
    private Long id;
    private Long profileUserId;
    private Long authorId;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String content;
    private Instant createdAt;
}

