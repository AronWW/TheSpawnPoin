package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.support.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findByTicketIdOrderBySentAtAsc(Long ticketId);
}

