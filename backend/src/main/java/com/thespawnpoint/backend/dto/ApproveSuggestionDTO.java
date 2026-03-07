package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveSuggestionDTO {

    @NotBlank(message = "Назва гри обов'язкова")
    private String name;

    private String genre;
    private Short releaseYear;
    private String imageUrl;

    @NotNull(message = "Максимальний розмір лобі обов'язковий")
    private Integer maxPartySize;
}

