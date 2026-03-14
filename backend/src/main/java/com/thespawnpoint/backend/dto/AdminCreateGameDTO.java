package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCreateGameDTO {

    @NotBlank(message = "Назва гри обов'язкова")
    private String name;

    private String genre;
    private Short releaseYear;
    private String imageUrl;

    @NotNull(message = "Максимальний розмір пати обов'язковий")
    private Integer maxPartySize;
}

