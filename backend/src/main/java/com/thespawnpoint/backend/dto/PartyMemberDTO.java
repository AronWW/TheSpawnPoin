package com.thespawnpoint.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PartyMemberDTO {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    @JsonProperty("isCreator")
    private Boolean creator;
    private Instant joinedAt;
}
