package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.*;
import com.thespawnpoint.backend.entity.game.Game;
import com.thespawnpoint.backend.entity.game.SuggestionStatus;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.report.ReportStatus;
import com.thespawnpoint.backend.entity.support.TicketStatus;
import com.thespawnpoint.backend.entity.unban.UnbanRequestStatus;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final GameRepository gameRepository;
    private final GameSuggestionRepository gameSuggestionRepository;
    private final PartyRequestRepository partyRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserReportRepository userReportRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final UnbanRequestRepository unbanRequestRepository;

    public AdminDashboardDTO getDashboard() {
        return AdminDashboardDTO.builder()
                .totalUsers(userRepository.count())
                .bannedUsers(userRepository.countByBannedTrue())
                .totalGames(gameRepository.count())
                .openParties(partyRequestRepository.countByIsOpen(true))
                .pendingSuggestions(gameSuggestionRepository.countByStatus(SuggestionStatus.PENDING))
                .openReports(userReportRepository.countByStatus(ReportStatus.OPEN))
                .openTickets(supportTicketRepository.countByStatus(TicketStatus.OPEN))
                .pendingUnbanRequests(unbanRequestRepository.countByStatus(UnbanRequestStatus.PENDING))
                .build();
    }

    public Page<AdminUserDTO> getAllUsers(String query, Pageable pageable) {
        String q = (query != null && !query.isBlank()) ? query : null;
        return userRepository.searchForAdmin(q, pageable).map(this::toAdminUserDTO);
    }

    public Page<AdminUserDTO> getBannedUsers(Pageable pageable) {
        return userRepository.findByBannedTrue(pageable).map(this::toAdminUserDTO);
    }

    public AdminUserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Користувача не знайдено"));
        return toAdminUserDTO(user);
    }

    @Transactional
    public AdminUserDTO banUser(Long id, String reason, User admin) {
        if (admin.getId().equals(id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Не можна забанити себе");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Користувача не знайдено"));

        if (user.isBanned()) {
            throw new ApiException(HttpStatus.CONFLICT, "Користувач вже забанений");
        }

        user.setBanned(true);
        user.setBanReason(reason);
        user.setBannedAt(Instant.now());
        userRepository.save(user);


        return toAdminUserDTO(user);
    }

    @Transactional
    public AdminUserDTO unbanUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Користувача не знайдено"));

        if (!user.isBanned()) {
            throw new ApiException(HttpStatus.CONFLICT, "Користувач не забанений");
        }

        user.setBanned(false);
        user.setBanReason(null);
        user.setBannedAt(null);
        userRepository.save(user);
        return toAdminUserDTO(user);
    }

    @Transactional
    public void deleteUser(Long id, User admin) {
        if (admin.getId().equals(id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Не можна видалити себе");
        }

        if (!userRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Користувача не знайдено");
        }

        userRepository.deleteById(id);
    }

    @Transactional
    public GameDTO createGame(AdminCreateGameDTO dto) {
        if (gameRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ApiException(HttpStatus.CONFLICT, "Гра з такою назвою вже існує");
        }

        Game game = Game.builder()
                .name(dto.getName())
                .genre(dto.getGenre())
                .releaseYear(dto.getReleaseYear())
                .imageUrl(dto.getImageUrl())
                .maxPartySize(dto.getMaxPartySize())
                .build();

        gameRepository.save(game);
        return toGameDTO(game);
    }

    @Transactional
    public GameDTO updateGame(Long id, AdminUpdateGameDTO dto) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Гру не знайдено"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            if (!game.getName().equalsIgnoreCase(dto.getName())
                    && gameRepository.existsByNameIgnoreCase(dto.getName())) {
                throw new ApiException(HttpStatus.CONFLICT, "Гра з такою назвою вже існує");
            }
            game.setName(dto.getName());
        }
        if (dto.getGenre() != null) game.setGenre(dto.getGenre());
        if (dto.getReleaseYear() != null) game.setReleaseYear(dto.getReleaseYear());
        if (dto.getImageUrl() != null) game.setImageUrl(dto.getImageUrl());
        if (dto.getMaxPartySize() != null) game.setMaxPartySize(dto.getMaxPartySize());

        gameRepository.save(game);
        return toGameDTO(game);
    }

    @Transactional
    public void deleteGame(Long id) {
        if (!gameRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Гру не знайдено");
        }
        gameRepository.deleteById(id);
    }

    public Page<AdminActivePartyDTO> getActiveParties(Pageable pageable) {
        return partyRequestRepository.findByIsOpenOrderByCreatedAtDesc(true, pageable)
                .map(this::toAdminActivePartyDTO);
    }

    private AdminUserDTO toAdminUserDTO(User u) {
        String avatarUrl = profileRepository.findByUserId(u.getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        return AdminUserDTO.builder()
                .id(u.getId())
                .displayName(u.getDisplayName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .status(u.getStatus().name())
                .emailVerified(u.isEmailVerified())
                .banned(u.isBanned())
                .banReason(u.getBanReason())
                .bannedAt(u.getBannedAt())
                .lastSeen(u.getLastSeen())
                .createdAt(u.getCreatedAt())
                .avatarUrl(avatarUrl)
                .build();
    }

    private AdminActivePartyDTO toAdminActivePartyDTO(PartyRequest p) {
        int count = partyMemberRepository.countByPartyRequestId(p.getId());
        return AdminActivePartyDTO.builder()
                .id(p.getId())
                .gameName(p.getGame().getName())
                .gameImageUrl(p.getGame().getImageUrl())
                .creatorDisplayName(p.getCreator().getDisplayName())
                .currentMembers(count)
                .maxMembers(p.getMaxMembers())
                .language(p.getLanguage())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private GameDTO toGameDTO(Game game) {
        return GameDTO.builder()
                .id(game.getId())
                .name(game.getName())
                .genre(game.getGenre())
                .releaseYear(game.getReleaseYear())
                .imageUrl(game.getImageUrl())
                .maxPartySize(game.getMaxPartySize())
                .build();
    }
}

