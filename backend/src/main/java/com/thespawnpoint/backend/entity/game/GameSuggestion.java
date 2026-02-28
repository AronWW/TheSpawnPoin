package com.thespawnpoint.backend.entity.game;

import com.thespawnpoint.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "game_suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "suggested_by", nullable = false)
    private User suggestedBy;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}

