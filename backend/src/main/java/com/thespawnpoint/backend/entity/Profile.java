package com.thespawnpoint.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "platforms", columnDefinition = "varchar(20)[]")
    private List<String> platforms;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "play_style", length = 20)
    private PlayStyle playStyle;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "languages", columnDefinition = "varchar(50)[]")
    private List<String> languages;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "region", length = 100)
    private String region;
}

