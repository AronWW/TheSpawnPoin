package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TicketDTO {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private String subject;
    private String status;
    private Instant createdAt;
    private Instant closedAt;
}

