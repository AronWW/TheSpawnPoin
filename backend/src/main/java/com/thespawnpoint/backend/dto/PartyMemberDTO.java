package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PartyMemberDTO {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    private boolean isCreator;
    private Instant joinedAt;
}

