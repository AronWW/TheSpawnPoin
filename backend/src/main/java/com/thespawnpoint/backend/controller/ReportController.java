package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.CreateReportDTO;
import com.thespawnpoint.backend.dto.ReportDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportDTO> createReport(
            @Valid @RequestBody CreateReportDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reportService.createReport(user, dto));
    }
}

