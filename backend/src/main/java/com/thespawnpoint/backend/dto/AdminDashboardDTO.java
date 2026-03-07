package com.thespawnpoint.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardDTO {
    private long totalUsers;
    private long bannedUsers;
    private long totalGames;
    private long openParties;
    private long pendingSuggestions;
    private long openReports;
    private long openTickets;
    private long pendingUnbanRequests;
}

