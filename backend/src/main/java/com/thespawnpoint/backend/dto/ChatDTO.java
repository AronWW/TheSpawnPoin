package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ChatDTO {
    private Long id;
    private boolean isGroup;
    private boolean partyLinked;
    private String title;
    private Long partyId;

    // DM fields (null for group chats)
    private String partnerEmail;
    private String partnerDisplayName;
    private String partnerStatus;
    private String partnerLastSeen;

    // Group fields (null for DM chats)
    private List<ChatParticipantDTO> participants;

    // Common
    private String lastMessage;
    private Instant lastMessageAt;
    private int unreadCount;
}

