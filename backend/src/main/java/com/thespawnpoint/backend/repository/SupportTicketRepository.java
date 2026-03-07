package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.support.SupportTicket;
import com.thespawnpoint.backend.entity.support.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(TicketStatus status);
}

