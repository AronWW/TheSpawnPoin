package com.thespawnpoint.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "genre", length = 50)
    private String genre;

    @Column(name = "release_year")
    private Short releaseYear;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "max_party_size", nullable = false)
    @Builder.Default
    private Integer maxPartySize = 5;
}

