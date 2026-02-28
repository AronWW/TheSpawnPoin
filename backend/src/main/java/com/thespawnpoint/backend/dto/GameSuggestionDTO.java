package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class GameSuggestionDTO {
    private Long id;
    private String name;
    private String genre;
    private Short releaseYear;
    private String imageUrl;
    private Integer maxPartySize;
    private String status;
    private String adminComment;
    private Long suggestedByUserId;
    private String suggestedByDisplayName;
    private Instant createdAt;
    private Instant reviewedAt;
}

