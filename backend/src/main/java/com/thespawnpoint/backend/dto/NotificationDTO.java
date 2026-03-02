package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String type;
    private String message;
    private Long referenceId;
    private boolean read;
    private Instant createdAt;
}

