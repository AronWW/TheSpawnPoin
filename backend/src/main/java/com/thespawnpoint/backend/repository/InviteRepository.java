package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.social.Invite;
import com.thespawnpoint.backend.entity.social.InviteStatus;
import com.thespawnpoint.backend.entity.social.InviteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {

    List<Invite> findByReceiverId(Long receiverId);

    List<Invite> findBySenderId(Long senderId);

    List<Invite> findByReceiverIdAndStatus(Long receiverId, InviteStatus status);

    List<Invite> findByReceiverIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long receiverId, InviteType type, InviteStatus status);

    List<Invite> findBySenderIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long senderId, InviteType type, InviteStatus status);

    @Query("""
            SELECT i FROM Invite i
            WHERE i.type = :type AND i.status = :status
              AND ((i.sender.id = :userId1 AND i.receiver.id = :userId2)
                OR (i.sender.id = :userId2 AND i.receiver.id = :userId1))
            """)
    Optional<Invite> findPendingBetween(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            @Param("type") InviteType type,
            @Param("status") InviteStatus status);
}

