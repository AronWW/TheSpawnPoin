package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.GameDTO;
import com.thespawnpoint.backend.dto.GameSuggestionDTO;
import com.thespawnpoint.backend.dto.SuggestGameDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.GameService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/games")
    public ResponseEntity<List<GameDTO>> getGames(@RequestParam(required = false) String q) {
        List<GameDTO> games = (q != null && !q.isBlank())
                ? gameService.searchGames(q)
                : gameService.getAllGames();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/games/search")
    public ResponseEntity<Page<GameDTO>> searchGames(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(gameService.searchGamesPaged(q, genre, PageRequest.of(page, size)));
    }

    @GetMapping("/games/{id}")
    public ResponseEntity<GameDTO> getGame(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getGameById(id));
    }

    @GetMapping("/users/me/games")
    public ResponseEntity<List<GameDTO>> getMyGames(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gameService.getUserGames(user.getId()));
    }

    @GetMapping("/users/{userId}/games")
    public ResponseEntity<List<GameDTO>> getUserGames(@PathVariable Long userId) {
        return ResponseEntity.ok(gameService.getUserGames(userId));
    }

    @PostMapping("/users/me/games/{gameId}")
    public ResponseEntity<Map<String, String>> addGame(
            @PathVariable Long gameId,
            @AuthenticationPrincipal User user) {
        gameService.addGameToUser(user, gameId);
        return ResponseEntity.ok(Map.of("message", "Game added"));
    }

    @DeleteMapping("/users/me/games/{gameId}")
    public ResponseEntity<Map<String, String>> removeGame(
            @PathVariable Long gameId,
            @AuthenticationPrincipal User user) {
        gameService.removeGameFromUser(user, gameId);
        return ResponseEntity.ok(Map.of("message", "Game removed"));
    }

    @PostMapping("/game-suggestions")
    public ResponseEntity<GameSuggestionDTO> suggestGame(
            @Valid @RequestBody SuggestGameDTO dto,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gameService.suggestGame(user, dto));
    }

    @GetMapping("/game-suggestions/my")
    public ResponseEntity<List<GameSuggestionDTO>> mySuggestions(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gameService.getMySuggestions(user));
    }

    @GetMapping("/admin/game-suggestions")
    public ResponseEntity<List<GameSuggestionDTO>> pendingSuggestions() {
        return ResponseEntity.ok(gameService.getPendingSuggestions());
    }

    @PostMapping("/admin/game-suggestions/{id}/approve")
    public ResponseEntity<GameDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.approveSuggestion(id));
    }

    @PostMapping("/admin/game-suggestions/{id}/reject")
    public ResponseEntity<GameSuggestionDTO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(gameService.rejectSuggestion(id, comment));
    }
}

