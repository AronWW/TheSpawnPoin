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
            WHERE pm.user.id = :userId AND pr.isOpen = true
            """)
    List<PartyMember> findActivePartiesByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END
            FROM PartyMember pm
            WHERE pm.user.id = :userId AND pm.partyRequest.isOpen = true
            """)
    boolean existsActivePartyForUser(@Param("userId") Long userId);

    Optional<PartyMember> findFirstByPartyRequestIdOrderByJoinedAtAsc(Long partyRequestId);
}
