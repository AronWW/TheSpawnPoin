package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileDTO {

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

    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;
}

