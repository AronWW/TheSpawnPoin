package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReactionDTO {
    private String emoji;
    private int count;
    private List<String> userEmails;
}

