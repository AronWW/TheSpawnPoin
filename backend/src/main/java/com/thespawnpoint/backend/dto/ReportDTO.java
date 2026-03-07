package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReportDTO {
    private Long id;
    private Long reporterId;
    private String reporterDisplayName;
    private Long reportedUserId;
    private String reportedUserDisplayName;
    private String reason;
    private String description;
    private String status;
    private String adminComment;
    private Instant createdAt;
    private Instant reviewedAt;
}

