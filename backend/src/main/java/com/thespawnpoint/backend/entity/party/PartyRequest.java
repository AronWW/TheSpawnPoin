package com.thespawnpoint.backend.entity.party;

import com.thespawnpoint.backend.entity.chat.Chat;
import com.thespawnpoint.backend.entity.game.Game;
import com.thespawnpoint.backend.entity.user.PlayStyle;
import com.thespawnpoint.backend.entity.user.SkillLevel;
import com.thespawnpoint.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "party_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_time")
    private Instant eventTime;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "platform", columnDefinition = "varchar(20)[]")
    private List<String> platform;

    @Column(name = "language", length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "play_style", length = 20)
    private PlayStyle playStyle;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

