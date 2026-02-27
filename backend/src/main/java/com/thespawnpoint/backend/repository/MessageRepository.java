package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.Chat;
import com.thespawnpoint.backend.entity.Message;
import com.thespawnpoint.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatOrderBySentAtDesc(Chat chat, Pageable pageable);

    Optional<Message> findFirstByChatOrderBySentAtDesc(Chat chat);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat = :chat AND m.sender <> :user AND m.read = false")
    int countUnreadInChat(@Param("chat") Chat chat, @Param("user") User user);

    @Modifying
    @Query("UPDATE Message m SET m.read = true WHERE m.chat = :chat AND m.sender <> :reader AND m.read = false")
    void markAsReadInChat(@Param("chat") Chat chat, @Param("reader") User reader);
}
