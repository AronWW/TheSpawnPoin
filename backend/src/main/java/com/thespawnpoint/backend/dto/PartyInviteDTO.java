package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PartyInviteDTO {

    private Long inviteId;

    private Long senderId;
    private String senderDisplayName;
    private String senderAvatarUrl;

    private Long receiverId;
    private String receiverDisplayName;
    private String receiverAvatarUrl;

    private Long partyId;
    private String gameName;
    private String gameImageUrl;

    private String status;
    private Instant createdAt;
}

