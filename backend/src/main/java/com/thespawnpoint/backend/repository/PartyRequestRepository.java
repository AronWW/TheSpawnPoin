package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Використовується для /api/parties (без пагінації)
    @Query(value = """
            SELECT pr.* FROM party_requests pr
            WHERE pr.is_open = true
              AND (:gameId   IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:language   IS NULL OR pr.language    = :language)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
            ORDER BY pr.created_at DESC
            """, nativeQuery = true)
    List<PartyRequest> findOpenWithFilters(
            @Param("gameId")     Long gameId,
            @Param("skillLevel") String skillLevel,
            @Param("playStyle")  String playStyle,
            @Param("language")   String language,
            @Param("platform")   String platform
    );

    // Використовується для /api/parties/search (з пагінацією)
    @Query(value = """
            SELECT pr.* FROM party_requests pr
            WHERE pr.is_open = true
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:language   IS NULL OR pr.language    = :language)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
            ORDER BY pr.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM party_requests pr
            WHERE pr.is_open = true
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:language   IS NULL OR pr.language    = :language)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
            """,
            nativeQuery = true)
    Page<PartyRequest> findOpenWithFiltersPaged(
            @Param("gameId")     Long gameId,
            @Param("skillLevel") String skillLevel,
            @Param("playStyle")  String playStyle,
            @Param("language")   String language,
            @Param("platform")   String platform,
            Pageable pageable
    );
}