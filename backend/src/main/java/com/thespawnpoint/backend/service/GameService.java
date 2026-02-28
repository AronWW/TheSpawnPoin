package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.GameDTO;
import com.thespawnpoint.backend.dto.GameSuggestionDTO;
import com.thespawnpoint.backend.dto.SuggestGameDTO;
import com.thespawnpoint.backend.entity.*;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.GameRepository;
import com.thespawnpoint.backend.repository.GameSuggestionRepository;
import com.thespawnpoint.backend.repository.UserGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final UserGameRepository userGameRepository;
    private final GameSuggestionRepository gameSuggestionRepository;

    public List<GameDTO> getAllGames() {
        return gameRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<GameDTO> searchGames(String query) {
        return gameRepository.findByNameContainingIgnoreCase(query).stream()
                .map(this::toDTO)
                .toList();
    }

    public GameDTO getGameById(Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Game not found"));
        return toDTO(game);
    }

    public List<GameDTO> getUserGames(Long userId) {
        return userGameRepository.findByUserId(userId).stream()
                .map(ug -> toDTO(ug.getGame()))
                .toList();
    }

    @Transactional
    public void addGameToUser(User user, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Game not found"));

        if (userGameRepository.existsByUserIdAndGameId(user.getId(), gameId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Game already added");
        }

        userGameRepository.save(UserGame.builder()
                .user(user)
                .game(game)
                .build());
    }

    @Transactional
    public void removeGameFromUser(User user, Long gameId) {
        if (!userGameRepository.existsByUserIdAndGameId(user.getId(), gameId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Game not in your list");
        }
        userGameRepository.deleteByUserIdAndGameId(user.getId(), gameId);
    }


    @Transactional
    public GameSuggestionDTO suggestGame(User user, SuggestGameDTO dto) {
        if (gameRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Game already exists in the catalog");
        }

        if (gameSuggestionRepository.existsByNameIgnoreCaseAndStatus(dto.getName(), SuggestionStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "A suggestion for this game is already pending review");
        }

        GameSuggestion suggestion = GameSuggestion.builder()
                .suggestedBy(user)
                .name(dto.getName())
                .genre(dto.getGenre())
                .releaseYear(dto.getReleaseYear())
                .imageUrl(dto.getImageUrl())
                .maxPartySize(dto.getMaxPartySize() != null ? dto.getMaxPartySize() : 5)
                .build();

        gameSuggestionRepository.save(suggestion);
        return toSuggestionDTO(suggestion);
    }

    public List<GameSuggestionDTO> getMySuggestions(User user) {
        return gameSuggestionRepository.findBySuggestedByIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toSuggestionDTO)
                .toList();
    }

    public List<GameSuggestionDTO> getPendingSuggestions() {
        return gameSuggestionRepository.findByStatus(SuggestionStatus.PENDING).stream()
                .map(this::toSuggestionDTO)
                .toList();
    }

    @Transactional
    public GameDTO approveSuggestion(Long suggestionId) {
        GameSuggestion suggestion = gameSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Suggestion not found"));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Suggestion already reviewed");
        }

        Game game = Game.builder()
                .name(suggestion.getName())
                .genre(suggestion.getGenre())
                .releaseYear(suggestion.getReleaseYear())
                .imageUrl(suggestion.getImageUrl())
                .maxPartySize(suggestion.getMaxPartySize())
                .build();
        gameRepository.save(game);

        suggestion.setStatus(SuggestionStatus.APPROVED);
        suggestion.setReviewedAt(Instant.now());
        gameSuggestionRepository.save(suggestion);

        return toDTO(game);
    }

    @Transactional
    public GameSuggestionDTO rejectSuggestion(Long suggestionId, String comment) {
        GameSuggestion suggestion = gameSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Suggestion not found"));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Suggestion already reviewed");
        }

        suggestion.setStatus(SuggestionStatus.REJECTED);
        suggestion.setAdminComment(comment);
        suggestion.setReviewedAt(Instant.now());
        gameSuggestionRepository.save(suggestion);

        return toSuggestionDTO(suggestion);
    }

    private GameDTO toDTO(Game game) {
        return GameDTO.builder()
                .id(game.getId())
                .name(game.getName())
                .genre(game.getGenre())
                .releaseYear(game.getReleaseYear())
                .imageUrl(game.getImageUrl())
                .maxPartySize(game.getMaxPartySize())
                .build();
    }

    private GameSuggestionDTO toSuggestionDTO(GameSuggestion s) {
        return GameSuggestionDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .genre(s.getGenre())
                .releaseYear(s.getReleaseYear())
                .imageUrl(s.getImageUrl())
                .maxPartySize(s.getMaxPartySize())
                .status(s.getStatus().name())
                .adminComment(s.getAdminComment())
                .suggestedByUserId(s.getSuggestedBy().getId())
                .suggestedByDisplayName(s.getSuggestedBy().getDisplayName())
                .createdAt(s.getCreatedAt())
                .reviewedAt(s.getReviewedAt())
                .build();
    }
}

