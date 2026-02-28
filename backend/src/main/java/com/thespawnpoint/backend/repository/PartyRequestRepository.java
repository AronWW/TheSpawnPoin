package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyRequestRepository extends JpaRepository<PartyRequest, Long> {

    List<PartyRequest> findByCreatorId(Long creatorId);

    List<PartyRequest> findByGameIdAndIsOpen(Long gameId, Boolean isOpen);

    List<PartyRequest> findByIsOpen(Boolean isOpen);
}

