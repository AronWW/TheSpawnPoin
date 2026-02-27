package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.ChatParticipant;
import com.thespawnpoint.backend.entity.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {

    List<ChatParticipant> findByIdChatId(Long chatId);

    List<ChatParticipant> findByIdUserId(Long userId);
}

