package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.chat.Chat;
import com.thespawnpoint.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("""
            SELECT c FROM Chat c
            WHERE c.isGroup = false
              AND EXISTS (SELECT cp FROM ChatParticipant cp WHERE cp.chat = c AND cp.user = :user1)
              AND EXISTS (SELECT cp FROM ChatParticipant cp WHERE cp.chat = c AND cp.user = :user2)
            """)
    Optional<Chat> findDmChat(@Param("user1") User user1, @Param("user2") User user2);

    @Query("""
            SELECT c FROM Chat c
            WHERE EXISTS (SELECT cp FROM ChatParticipant cp WHERE cp.chat = c AND cp.user = :user)
            ORDER BY c.createdAt DESC
            """)
    List<Chat> findAllByUser(@Param("user") User user);
}
