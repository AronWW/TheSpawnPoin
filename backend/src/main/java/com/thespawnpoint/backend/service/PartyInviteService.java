package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.PartyInviteDTO;
import com.thespawnpoint.backend.entity.party.PartyMember;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.social.*;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyInviteService {

    private final InviteRepository inviteRepository;
    private final PartyRequestRepository partyRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @Transactional
    public PartyInviteDTO sendPartyInvite(User sender, Long partyId, Long receiverId) {
        if (sender.getId().equals(receiverId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot invite yourself");
        }

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getIsOpen()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party is closed");
        }

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, sender.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this party");
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, receiverId)) {
            throw new ApiException(HttpStatus.CONFLICT, "User is already in this party");
        }

        int currentCount = partyMemberRepository.countByPartyRequestId(partyId);
        if (currentCount >= party.getMaxMembers()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party is full");
        }

        if (inviteRepository.findAnyPendingPartyInviteForUser(receiverId, partyId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "This user already has a pending invite to this party");
        }

        if (partyMemberRepository.existsActivePartyForUser(receiverId)) {
            throw new ApiException(HttpStatus.CONFLICT, "User already has an active party");
        }

        Invite invite = inviteRepository.save(Invite.builder()
                .sender(sender)
                .receiver(receiver)
                .type(InviteType.PARTY_INVITE)
                .status(InviteStatus.PENDING)
                .partyRequest(party)
                .build());

        notificationService.send(
                receiver,
                NotificationType.PARTY_INVITE,
                sender.getDisplayName() + " invited you to play " + party.getGame().getName(),
                invite.getId()
        );

        return toDTO(invite);
    }

    @Transactional
    public void acceptPartyInvite(User user, Long inviteId) {
        Invite invite = getValidPartyInvite(inviteId, user);

        PartyRequest party = invite.getPartyRequest();

        if (party == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party no longer exists");
        }

        if (!party.getIsOpen()) {
            invite.setStatus(InviteStatus.DECLINED);
            invite.setRespondedAt(Instant.now());
            inviteRepository.save(invite);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party is closed");
        }

        int currentCount = partyMemberRepository.countByPartyRequestId(party.getId());
        if (currentCount >= party.getMaxMembers()) {
            invite.setStatus(InviteStatus.DECLINED);
            invite.setRespondedAt(Instant.now());
            inviteRepository.save(invite);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party is full");
        }

        if (partyMemberRepository.existsActivePartyForUser(user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an active party");
        }

        if (partyMemberRepository.existsByPartyRequestIdAndUserId(party.getId(), user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already in this party");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setRespondedAt(Instant.now());
        inviteRepository.save(invite);

        partyMemberRepository.save(PartyMember.builder()
                .partyRequest(party)
                .user(user)
                .build());

        if (party.getChat() != null) {
            chatService.addParticipant(party.getChat().getId(), user);
        }

        currentCount++;
        if (currentCount >= party.getMaxMembers()) {
            party.setIsOpen(false);
            partyRequestRepository.save(party);
        }

        notificationService.send(
                invite.getSender(),
                NotificationType.PARTY_INVITE,
                user.getDisplayName() + " accepted your invite to " + party.getGame().getName(),
                party.getId()
        );
    }

    @Transactional
    public void declinePartyInvite(User user, Long inviteId) {
        Invite invite = getValidPartyInvite(inviteId, user);

        invite.setStatus(InviteStatus.DECLINED);
        invite.setRespondedAt(Instant.now());
        inviteRepository.save(invite);

        if (invite.getPartyRequest() != null) {
            notificationService.send(
                    invite.getSender(),
                    NotificationType.PARTY_INVITE,
                    user.getDisplayName() + " declined your invite to " + invite.getPartyRequest().getGame().getName(),
                    invite.getPartyRequest().getId()
            );
        }
    }

    @Transactional
    public void cancelPartyInvite(User sender, Long inviteId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getSender().getId().equals(sender.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your invite");
        }

        if (invite.getType() != InviteType.PARTY_INVITE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not a party invite");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite is no longer pending");
        }

        inviteRepository.delete(invite);
    }

    public List<PartyInviteDTO> getIncomingPartyInvites(User user) {
        return inviteRepository.findByReceiverIdAndTypeAndStatusOrderByCreatedAtDesc(
                        user.getId(), InviteType.PARTY_INVITE, InviteStatus.PENDING)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<PartyInviteDTO> getOutgoingPartyInvites(User user) {
        return inviteRepository.findBySenderIdAndTypeAndStatusOrderByCreatedAtDesc(
                        user.getId(), InviteType.PARTY_INVITE, InviteStatus.PENDING)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<PartyInviteDTO> getPartyInvites(Long partyId) {
        return inviteRepository.findByPartyRequestIdAndTypeAndStatus(
                        partyId, InviteType.PARTY_INVITE, InviteStatus.PENDING)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private Invite getValidPartyInvite(Long inviteId, User receiver) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getReceiver().getId().equals(receiver.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your invite");
        }

        if (invite.getType() != InviteType.PARTY_INVITE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not a party invite");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite is no longer pending");
        }

        return invite;
    }

    private PartyInviteDTO toDTO(Invite invite) {
        String senderAvatar = profileRepository.findByUserId(invite.getSender().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        String receiverAvatar = profileRepository.findByUserId(invite.getReceiver().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        PartyRequest party = invite.getPartyRequest();

        return PartyInviteDTO.builder()
                .inviteId(invite.getId())
                .senderId(invite.getSender().getId())
                .senderDisplayName(invite.getSender().getDisplayName())
                .senderAvatarUrl(senderAvatar)
                .receiverId(invite.getReceiver().getId())
                .receiverDisplayName(invite.getReceiver().getDisplayName())
                .receiverAvatarUrl(receiverAvatar)
                .partyId(party != null ? party.getId() : null)
                .gameName(party != null ? party.getGame().getName() : null)
                .gameImageUrl(party != null ? party.getGame().getImageUrl() : null)
                .status(invite.getStatus().name())
                .createdAt(invite.getCreatedAt())
                .build();
    }
}

