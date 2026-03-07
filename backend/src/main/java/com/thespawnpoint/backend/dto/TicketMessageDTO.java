package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TicketMessageDTO {
    private Long id;
    private Long senderId;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private boolean admin;
    private String content;
    private Instant sentAt;
}

