package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    List<PartyMember> findByPartyRequestId(Long partyRequestId);

    List<PartyMember> findByUserId(Long userId);

    boolean existsByPartyRequestIdAndUserId(Long partyRequestId, Long userId);

    void deleteByPartyRequestIdAndUserId(Long partyRequestId, Long userId);
}
