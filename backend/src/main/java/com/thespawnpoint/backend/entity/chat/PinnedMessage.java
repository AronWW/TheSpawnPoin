package com.thespawnpoint.backend.entity.chat;

import com.thespawnpoint.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "pinned_messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "message_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "pinned_by", nullable = false)
    private User pinnedBy;

    @Column(name = "pinned_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant pinnedAt = Instant.now();
}

