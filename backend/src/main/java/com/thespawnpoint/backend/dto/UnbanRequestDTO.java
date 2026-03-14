package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class UnbanRequestDTO {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private String userEmail;
    private String userAvatarUrl;
    private String reason;
    private String status;
    private String adminComment;
    private String banReason;
    private List<BanRecordDTO> banHistory;
    private Instant createdAt;
    private Instant reviewedAt;

    @Data
    @Builder
    public static class BanRecordDTO {
        private String banReason;
        private Instant createdAt;
        private String requestReason;
        private String requestStatus;
    }
}

