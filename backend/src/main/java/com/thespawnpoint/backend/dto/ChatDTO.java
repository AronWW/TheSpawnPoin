package com.thespawnpoint.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ChatDTO {
    private Long id;

    @JsonProperty("isGroup")
    @Builder.Default
    private Boolean group = false;

    @JsonProperty("isPartyLinked")
    @Builder.Default
    private Boolean partyLinkedFlag = false;

    private String title;
    private Long partyId;

    private String partnerEmail;
    private String partnerDisplayName;
    private String partnerStatus;
    private String partnerLastSeen;

    private List<ChatParticipantDTO> participants;

    private String lastMessage;
    private Instant lastMessageAt;
    private int unreadCount;

    @Builder.Default
    private boolean archived = false;

    @Builder.Default
    private boolean pinned = false;

    private Instant pinnedAt;
}
