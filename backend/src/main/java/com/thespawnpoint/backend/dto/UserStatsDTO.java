package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsDTO {
    private long completedGames;
    private long partiesCreated;
    private long partiesJoined;
    private double hoursPlayed;
    private String favoriteGameName;
    private String favoriteGameImageUrl;
    private Long favoriteGameId;
}

