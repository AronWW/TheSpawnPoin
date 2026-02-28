package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRequestRepository extends JpaRepository<PartyRequest, Long> {

    List<PartyRequest> findByCreatorId(Long creatorId);

    List<PartyRequest> findByGameIdAndIsOpen(Long gameId, Boolean isOpen);

    List<PartyRequest> findByIsOpen(Boolean isOpen);

    boolean existsByCreatorIdAndIsOpen(Long creatorId, Boolean isOpen);

    Optional<PartyRequest> findByChatId(Long chatId);

    @Query("""
            SELECT pr FROM PartyRequest pr
            JOIN FETCH pr.game
            JOIN FETCH pr.creator
            WHERE pr.isOpen = true
              AND (:gameId IS NULL OR pr.game.id = :gameId)
              AND (:skillLevel IS NULL OR pr.skillLevel = :skillLevel)
              AND (:playStyle IS NULL OR pr.playStyle = :playStyle)
              AND (:language IS NULL OR pr.language = :language)
            ORDER BY pr.createdAt DESC
            """)
    List<PartyRequest> findOpenWithFilters(
            @Param("gameId") Long gameId,
            @Param("skillLevel") String skillLevel,
            @Param("playStyle") String playStyle,
            @Param("language") String language
    );
}

