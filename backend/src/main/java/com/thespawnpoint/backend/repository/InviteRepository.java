package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.social.Invite;
import com.thespawnpoint.backend.entity.social.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {

    List<Invite> findByReceiverId(Long receiverId);

    List<Invite> findBySenderId(Long senderId);

    List<Invite> findByReceiverIdAndStatus(Long receiverId, InviteStatus status);
}

