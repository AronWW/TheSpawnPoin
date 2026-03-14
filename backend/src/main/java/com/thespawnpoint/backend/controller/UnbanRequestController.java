package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.CreateUnbanRequestDTO;
import com.thespawnpoint.backend.dto.UnbanRequestDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.UnbanRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/unban-requests")
@RequiredArgsConstructor
public class UnbanRequestController {

    private final UnbanRequestService unbanRequestService;

    @PostMapping
    public ResponseEntity<UnbanRequestDTO> create(
            @Valid @RequestBody CreateUnbanRequestDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(unbanRequestService.createRequest(user, dto.getReason()));
    }

    @GetMapping("/my")
    public ResponseEntity<UnbanRequestDTO> getMyLatest(
            @AuthenticationPrincipal User user) {
        UnbanRequestDTO request = unbanRequestService.getMyLatestRequest(user);
        return ResponseEntity.ok(request);
    }
}

