package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProfileDTO {
    private Long userId;
    private String email;
    private String displayName;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private LocalDate birthDate;
    private List<String> platforms;
    private String skillLevel;
    private String playStyle;
    private List<String> languages;
    private String country;
    private String region;
    private String discord;
    private String steam;
    private String twitch;
    private String xbox;
    private String playstation;
    private String nintendo;
}