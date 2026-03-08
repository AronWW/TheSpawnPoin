package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.social.Invite;
import com.thespawnpoint.backend.entity.social.InviteStatus;
import com.thespawnpoint.backend.entity.social.InviteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    @Query("""
            SELECT i FROM Invite i
            WHERE i.type = 'PARTY_INVITE' AND i.status = 'PENDING'
              AND i.sender.id = :senderId AND i.receiver.id = :receiverId
              AND i.partyRequest.id = :partyId
            """)
    Optional<Invite> findPendingPartyInvite(
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId,
            @Param("partyId") Long partyId);

    @Query("""
            SELECT i FROM Invite i
            WHERE i.type = 'PARTY_INVITE' AND i.status = 'PENDING'
              AND i.receiver.id = :receiverId
              AND i.partyRequest.id = :partyId
            """)
    Optional<Invite> findAnyPendingPartyInviteForUser(
            @Param("receiverId") Long receiverId,
            @Param("partyId") Long partyId);

    List<Invite> findByPartyRequestIdAndTypeAndStatus(
            Long partyRequestId, InviteType type, InviteStatus status);

    @Modifying
    @Query("""
            UPDATE Invite i SET i.status = :expiredStatus, i.respondedAt = :now
            WHERE i.type = 'PARTY_INVITE' AND i.status = 'PENDING'
              AND i.createdAt < :cutoff
            """)
    int expireOldPartyInvites(@Param("cutoff") Instant cutoff,
                              @Param("now") Instant now,
                              @Param("expiredStatus") InviteStatus expiredStatus);
}

