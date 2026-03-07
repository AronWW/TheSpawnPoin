package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminActivePartyDTO {
    private Long id;
    private String gameName;
    private String gameImageUrl;
    private String creatorDisplayName;
    private int currentMembers;
    private int maxMembers;
    private String language;
    private Instant createdAt;
}

