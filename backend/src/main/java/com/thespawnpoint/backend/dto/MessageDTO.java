package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageDTO {
    private Long id;
    private Long chatId;
    private String senderEmail;
    private String senderName;
    private String senderAvatarUrl;
    private String content;
    private Instant sentAt;
    private boolean read;
    private boolean system;
}
