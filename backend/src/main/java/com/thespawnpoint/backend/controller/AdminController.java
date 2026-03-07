package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.*;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.AdminService;
import com.thespawnpoint.backend.service.GameService;
import com.thespawnpoint.backend.service.ReportService;
import com.thespawnpoint.backend.service.SupportService;
import com.thespawnpoint.backend.service.UnbanRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final GameService gameService;
    private final ReportService reportService;
    private final SupportService supportService;
    private final UnbanRequestService unbanRequestService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDTO>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(q, PageRequest.of(page, size)));
    }

    @GetMapping("/users/banned")
    public ResponseEntity<Page<AdminUserDTO>> getBannedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getBannedUsers(PageRequest.of(page, size)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<AdminUserDTO> banUser(
            @PathVariable Long id,
            @RequestBody(required = false) BanUserDTO dto,
            @AuthenticationPrincipal User admin) {
        String reason = dto != null ? dto.getReason() : null;
        return ResponseEntity.ok(adminService.banUser(id, reason, admin));
    }

    @PostMapping("/users/{id}/unban")
    public ResponseEntity<AdminUserDTO> unbanUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unbanUser(id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        adminService.deleteUser(id, admin);
        return ResponseEntity.ok(Map.of("message", "Користувача видалено"));
    }

    @GetMapping("/games")
    public ResponseEntity<Page<GameDTO>> getGames(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gameService.searchGamesPaged(q, genre, PageRequest.of(page, size)));
    }

    @PostMapping("/games")
    public ResponseEntity<GameDTO> createGame(@Valid @RequestBody AdminCreateGameDTO dto) {
        return ResponseEntity.ok(adminService.createGame(dto));
    }

    @PutMapping("/games/{id}")
    public ResponseEntity<GameDTO> updateGame(
            @PathVariable Long id,
            @RequestBody AdminUpdateGameDTO dto) {
        return ResponseEntity.ok(adminService.updateGame(id, dto));
    }

    @DeleteMapping("/games/{id}")
    public ResponseEntity<Map<String, String>> deleteGame(@PathVariable Long id) {
        adminService.deleteGame(id);
        return ResponseEntity.ok(Map.of("message", "Гру видалено"));
    }

    @GetMapping("/game-suggestions")
    public ResponseEntity<Page<GameSuggestionDTO>> getSuggestions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gameService.getSuggestionsPaged(status, PageRequest.of(page, size)));
    }

    @PostMapping("/game-suggestions/{id}/approve")
    public ResponseEntity<GameDTO> approveSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApproveSuggestionDTO dto) {
        return ResponseEntity.ok(gameService.approveSuggestion(id, dto));
    }

    @PostMapping("/game-suggestions/{id}/reject")
    public ResponseEntity<GameSuggestionDTO> rejectSuggestion(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(gameService.rejectSuggestion(id, comment));
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportDTO>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportService.getReports(status, PageRequest.of(page, size)));
    }

    @PostMapping("/reports/{id}/review")
    public ResponseEntity<ReportDTO> reviewReport(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReportDTO dto) {
        return ResponseEntity.ok(reportService.reviewReport(id, dto));
    }

    @GetMapping("/tickets")
    public ResponseEntity<Page<TicketDTO>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(supportService.getAllTickets(status, PageRequest.of(page, size)));
    }

    @GetMapping("/tickets/{id}/messages")
    public ResponseEntity<List<TicketMessageDTO>> getTicketMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(supportService.getTicketMessages(id, admin));
    }

    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<TicketMessageDTO> replyToTicket(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User admin) {
        String content = body.get("content");
        return ResponseEntity.ok(supportService.replyToTicket(id, admin, content));
    }

    @PostMapping("/tickets/{id}/status")
    public ResponseEntity<TicketDTO> changeTicketStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(supportService.changeTicketStatus(id, body.get("status")));
    }

    @GetMapping("/unban-requests")
    public ResponseEntity<Page<UnbanRequestDTO>> getUnbanRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(unbanRequestService.getAll(status, PageRequest.of(page, size)));
    }

    @PostMapping("/unban-requests/{id}/review")
    public ResponseEntity<UnbanRequestDTO> reviewUnbanRequest(
            @PathVariable Long id,
            @Valid @RequestBody ReviewUnbanRequestDTO dto) {
        return ResponseEntity.ok(unbanRequestService.reviewRequest(id, dto));
    }

    @GetMapping("/parties/active")
    public ResponseEntity<Page<AdminActivePartyDTO>> getActiveParties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getActiveParties(PageRequest.of(page, size)));
    }
}


