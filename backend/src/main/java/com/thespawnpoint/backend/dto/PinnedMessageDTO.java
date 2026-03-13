package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PinnedMessageDTO {
    private Long id;
    private Long messageId;
    private Long chatId;
    private String content;
    private String senderName;
    private String pinnedByName;
    private Instant pinnedAt;
}

