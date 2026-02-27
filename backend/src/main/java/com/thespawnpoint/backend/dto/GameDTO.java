package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameDTO {
    private Long id;
    private String name;
    private String genre;
    private Short releaseYear;
    private String imageUrl;
    private Integer maxPartySize;
}

