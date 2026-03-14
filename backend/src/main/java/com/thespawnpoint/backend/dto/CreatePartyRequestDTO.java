package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreatePartyRequestDTO {

    @NotNull(message = "Game ID is required")
    private Long gameId;

    private String title;

    private String description;

    private Instant eventTime;

    private List<String> platform;

    private List<String> languages;

    private String skillLevel;

    private String playStyle;

    private List<String> tags;

    private String region;

    private Integer maxMembers;
}

