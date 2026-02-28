package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuggestGameDTO {

    @NotBlank(message = "Game name is required")
    @Size(max = 100, message = "Game name must be at most 100 characters")
    private String name;

    @Size(max = 50, message = "Genre must be at most 50 characters")
    private String genre;

    private Short releaseYear;

    @Size(max = 500, message = "Image URL must be at most 500 characters")
    private String imageUrl;

    private Integer maxPartySize;
}
