package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.party.PartyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRequestRepository extends JpaRepository<PartyRequest, Long> {

    List<PartyRequest> findByCreatorId(Long creatorId);

    long countByStatus(PartyStatus status);

    Page<PartyRequest> findByStatusOrderByCreatedAtDesc(PartyStatus status, Pageable pageable);

    boolean existsByCreatorIdAndStatusIn(Long creatorId, List<PartyStatus> statuses);

    Optional<PartyRequest> findByChatId(Long chatId);

    @Query(value = """
            SELECT pr.* FROM party_requests pr
            WHERE pr.status = 'OPEN'
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
            ORDER BY pr.created_at DESC
            """, nativeQuery = true)
    List<PartyRequest> findOpenWithFilters(
            @Param("gameId")     Long gameId,
            @Param("skillLevel") String skillLevel,
            @Param("playStyle")  String playStyle,
            @Param("platform")   String platform
    );

    @Query(value = """
            SELECT pr.* FROM party_requests pr
            JOIN games g ON g.id = pr.game_id
            WHERE pr.status = 'OPEN'
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
              AND (:q          IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.title, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY pr.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM party_requests pr
            JOIN games g ON g.id = pr.game_id
            WHERE pr.status = 'OPEN'
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
              AND (:q          IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.title, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """,
            nativeQuery = true)
    Page<PartyRequest> findOpenWithFiltersPaged(
            @Param("gameId")     Long gameId,
            @Param("skillLevel") String skillLevel,
            @Param("playStyle")  String playStyle,
            @Param("platform")   String platform,
            @Param("q")          String q,
            Pageable pageable
    );

    @Query(value = """
            SELECT pr.* FROM party_requests pr
            JOIN party_members pm ON pm.party_request_id = pr.id
            WHERE pm.user_id = :userId
              AND pr.status IN ('COMPLETED', 'CANCELLED')
            ORDER BY pr.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM party_requests pr
            JOIN party_members pm ON pm.party_request_id = pr.id
            WHERE pm.user_id = :userId
              AND pr.status IN ('COMPLETED', 'CANCELLED')
            """,
            nativeQuery = true)
    Page<PartyRequest> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE PartyRequest p SET p.status = 'CANCELLED', p.isOpen = false WHERE p.status = 'OPEN' AND p.createdAt < :cutoff")
    int cancelStaleOpenParties(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE PartyRequest p SET p.status = 'COMPLETED', p.isOpen = false, p.completedAt = CURRENT_TIMESTAMP, p.autoCompleted = true WHERE p.status = 'IN_GAME' AND p.startedAt < :cutoff")
    int completeStaleInGameParties(@Param("cutoff") Instant cutoff);
}
