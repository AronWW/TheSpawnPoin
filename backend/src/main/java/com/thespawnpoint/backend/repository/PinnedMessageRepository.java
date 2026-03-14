package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.chat.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, Long> {

    List<PinnedMessage> findByChatId(Long chatId);

    boolean existsByChatIdAndMessageId(Long chatId, Long messageId);

    @Modifying
    void deleteByChatIdAndMessageId(Long chatId, Long messageId);
}

