package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    List<PartyMember> findByPartyRequestId(Long partyRequestId);

    List<PartyMember> findByUserId(Long userId);

    boolean existsByPartyRequestIdAndUserId(Long partyRequestId, Long userId);

    void deleteByPartyRequestIdAndUserId(Long partyRequestId, Long userId);

    int countByPartyRequestId(Long partyRequestId);

    @Query("""
            SELECT pm FROM PartyMember pm
            WHERE pm.partyRequest.id = :partyRequestId
            ORDER BY pm.joinedAt ASC
            """)
    List<PartyMember> findByPartyRequestIdOrderByJoinedAtAsc(@Param("partyRequestId") Long partyRequestId);

    @Query("""
            SELECT pm FROM PartyMember pm
            JOIN FETCH pm.partyRequest pr
            JOIN FETCH pr.game
            WHERE pm.user.id = :userId AND pr.status IN (
                com.thespawnpoint.backend.entity.party.PartyStatus.OPEN,
                com.thespawnpoint.backend.entity.party.PartyStatus.FULL,
                com.thespawnpoint.backend.entity.party.PartyStatus.IN_GAME
            )
            """)
    List<PartyMember> findActivePartiesByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
            FROM PartyMember pm
            WHERE pm.user.id = :userId AND pm.partyRequest.status IN (
                com.thespawnpoint.backend.entity.party.PartyStatus.OPEN,
                com.thespawnpoint.backend.entity.party.PartyStatus.FULL,
                com.thespawnpoint.backend.entity.party.PartyStatus.IN_GAME
            )
            """)
    boolean existsActivePartyForUser(@Param("userId") Long userId);

    Optional<PartyMember> findFirstByPartyRequestIdOrderByJoinedAtAsc(Long partyRequestId);

    @Query("""
            SELECT COUNT(DISTINCT pm.partyRequest.id) FROM PartyMember pm
            WHERE pm.user.id = :userId
              AND pm.partyRequest.status = com.thespawnpoint.backend.entity.party.PartyStatus.COMPLETED
              AND pm.partyRequest.startedAt IS NOT NULL
              AND pm.partyRequest.completedAt IS NOT NULL
              AND pm.partyRequest.autoCompleted = false
            """)
    long countCompletedPartiesByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(DISTINCT pm.partyRequest.id) FROM PartyMember pm
            WHERE pm.user.id = :userId
              AND pm.partyRequest.status = com.thespawnpoint.backend.entity.party.PartyStatus.COMPLETED
              AND pm.partyRequest.creator.id = :userId
              AND pm.partyRequest.startedAt IS NOT NULL
              AND pm.partyRequest.completedAt IS NOT NULL
              AND pm.partyRequest.autoCompleted = false
            """)
    long countCompletedPartiesCreatedByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (pr.completed_at - pr.started_at))), 0)
            FROM party_members pm
            JOIN party_requests pr ON pr.id = pm.party_request_id
            WHERE pm.user_id = :userId
              AND pr.status = 'COMPLETED'
              AND pr.started_at IS NOT NULL
              AND pr.completed_at IS NOT NULL
              AND pr.auto_completed = false
            """, nativeQuery = true)
    long sumPlayTimeSecondsByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT pr.game_id
            FROM party_members pm
            JOIN party_requests pr ON pr.id = pm.party_request_id
            WHERE pm.user_id = :userId
              AND pr.status = 'COMPLETED'
              AND pr.started_at IS NOT NULL
              AND pr.completed_at IS NOT NULL
              AND pr.auto_completed = false
            GROUP BY pr.game_id
            ORDER BY COUNT(*) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findFavoriteGameIdByUserId(@Param("userId") Long userId);
}
