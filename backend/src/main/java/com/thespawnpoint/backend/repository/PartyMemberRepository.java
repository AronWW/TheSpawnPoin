package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.PartyMember;
import com.thespawnpoint.backend.entity.PartyMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyMemberRepository extends JpaRepository<PartyMember, PartyMemberId> {

    List<PartyMember> findByIdPartyRequestId(Long partyRequestId);

    List<PartyMember> findByIdUserId(Long userId);
}

