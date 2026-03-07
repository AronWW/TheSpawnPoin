package com.thespawnpoint.backend.dto;

import lombok.Data;

@Data
public class AdminUpdateGameDTO {
    private String name;
    private String genre;
    private Short releaseYear;
    private String imageUrl;
    private Integer maxPartySize;
}

