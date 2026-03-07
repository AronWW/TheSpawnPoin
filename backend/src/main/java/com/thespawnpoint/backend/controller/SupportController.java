package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.CreateTicketDTO;
import com.thespawnpoint.backend.dto.TicketDTO;
import com.thespawnpoint.backend.dto.TicketMessageDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/tickets")
    public ResponseEntity<TicketDTO> createTicket(
            @Valid @RequestBody CreateTicketDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportService.createTicket(user, dto));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<TicketDTO>> getMyTickets(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportService.getMyTickets(user));
    }

    @GetMapping("/tickets/{id}/messages")
    public ResponseEntity<List<TicketMessageDTO>> getTicketMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportService.getTicketMessages(id, user));
    }

    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<TicketMessageDTO> replyToTicket(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportService.replyToTicket(id, user, body.get("content")));
    }
}

