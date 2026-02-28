package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.CreatePartyRequestDTO;
import com.thespawnpoint.backend.dto.PartyRequestDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.PartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @PostMapping
    public ResponseEntity<PartyRequestDTO> createParty(
            @Valid @RequestBody CreatePartyRequestDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.createParty(user, dto));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<PartyRequestDTO> joinParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.joinParty(user, id));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        PartyRequestDTO result = partyService.leaveParty(user, id);
        if (result == null) {
            return ResponseEntity.ok(Map.of("message", "Party deleted (no members left)"));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PartyRequestDTO> closeParty(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.closeParty(user, id));
    }

    @GetMapping
    public ResponseEntity<List<PartyRequestDTO>> getOpenParties(
            @RequestParam(required = false) Long gameId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String skillLevel,
            @RequestParam(required = false) String playStyle,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(partyService.getOpenParties(gameId, platform, skillLevel, playStyle, language));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyRequestDTO> getParty(@PathVariable Long id) {
        return ResponseEntity.ok(partyService.getPartyById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PartyRequestDTO>> getMyParties(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(partyService.getMyParties(user));
    }
}

