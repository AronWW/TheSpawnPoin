package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

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

    private boolean deleted;

    private boolean edited;
    private Instant editedAt;

    private Long replyToId;
    private String replyToContent;
    private String replyToSenderName;

    private List<ReactionDTO> reactions;
}
