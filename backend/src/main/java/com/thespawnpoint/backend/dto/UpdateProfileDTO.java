package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileDTO {

    @Size(min = 2, max = 30, message = "Display name must be between 2 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\- ]+$", message = "Display name can only contain letters, numbers, spaces, hyphens and underscores")
    private String displayName;

    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private LocalDate birthDate;

    private List<String> platforms;

    private String skillLevel;

    private String playStyle;

    private List<String> languages;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    private String region;

    @Size(max = 100, message = "Discord must not exceed 100 characters")
    private String discord;

    @Size(max = 200, message = "Steam URL must not exceed 200 characters")
    private String steam;

    @Size(max = 200, message = "Twitch URL must not exceed 200 characters")
    private String twitch;

    @Size(max = 200, message = "Xbox gamertag must not exceed 200 characters")
    private String xbox;

    @Size(max = 200, message = "PlayStation ID must not exceed 200 characters")
    private String playstation;

    @Size(max = 200, message = "Nintendo friend code must not exceed 200 characters")
    private String nintendo;
}