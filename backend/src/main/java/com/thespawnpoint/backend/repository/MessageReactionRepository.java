package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.chat.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    List<MessageReaction> findByMessageId(Long messageId);

    List<MessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    @Modifying
    void deleteByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);
}

