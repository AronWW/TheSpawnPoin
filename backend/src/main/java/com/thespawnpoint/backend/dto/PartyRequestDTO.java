package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PartyRequestDTO {

    private Long id;

    private Long creatorId;
    private String creatorDisplayName;
    private String creatorAvatarUrl;

    private Long gameId;
    private String gameName;
    private String gameImageUrl;

    private Integer maxMembers;
    private Integer currentMembers;
    private Boolean isOpen;
    private String status;

    private String title;
    private String description;
    private Instant eventTime;

    private List<String> platform;
    private List<String> languages;
    private String skillLevel;
    private String playStyle;
    private List<String> tags;
    private String region;

    private List<PartyMemberDTO> members;

    private Long chatId;

    private Instant createdAt;
}

