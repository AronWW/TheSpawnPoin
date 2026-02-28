package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreatePartyRequestDTO {

    @NotNull(message = "Game ID is required")
    private Long gameId;

    private String description;

    private Instant eventTime;

    private List<String> platform;

    private String language;

    private String skillLevel;

    private String playStyle;
}

