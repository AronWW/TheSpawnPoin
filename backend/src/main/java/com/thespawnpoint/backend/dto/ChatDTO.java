package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ChatDTO {
    private Long id;
    private String partnerEmail;
    private String partnerDisplayName;
    private String partnerStatus;
    private String partnerLastSeen;
    private String lastMessage;
    private Instant lastMessageAt;
    private int unreadCount;
}

