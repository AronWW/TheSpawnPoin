package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.CreatePartyRequestDTO;
import com.thespawnpoint.backend.dto.PartyMemberDTO;
import com.thespawnpoint.backend.dto.PartyRequestDTO;
import com.thespawnpoint.backend.dto.UserStatsDTO;
import com.thespawnpoint.backend.entity.game.Game;
import com.thespawnpoint.backend.entity.party.PartyMember;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.party.PartyStatus;
import com.thespawnpoint.backend.entity.user.Language;
import com.thespawnpoint.backend.entity.user.PlayStyle;
import com.thespawnpoint.backend.entity.user.Platform;
import com.thespawnpoint.backend.entity.user.Region;
import com.thespawnpoint.backend.entity.user.SkillLevel;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.chat.Chat;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PartyService {

    private static final Set<PartyStatus> ACTIVE_STATUSES =
            Set.of(PartyStatus.OPEN, PartyStatus.FULL, PartyStatus.IN_GAME);

    private final PartyRequestRepository partyRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final GameRepository gameRepository;
    private final ProfileRepository profileRepository;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public PartyRequestDTO createParty(User creator, CreatePartyRequestDTO dto) {

        if (partyMemberRepository.existsActivePartyForUser(creator.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "У вас вже є активне лобі");
        }

        Game game = gameRepository.findById(dto.getGameId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Game not found"));

        validatePlatforms(dto.getPlatform());
        validateLanguages(dto.getLanguages());
        SkillLevel skillLevel = parseEnum(SkillLevel.class, dto.getSkillLevel(), "skill level");
        PlayStyle playStyle = parseEnum(PlayStyle.class, dto.getPlayStyle(), "play style");

        int maxMembers = game.getMaxPartySize();
        if (dto.getMaxMembers() != null) {
            if (dto.getMaxMembers() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Мінімум 2 гравці");
            }
            if (dto.getMaxMembers() > game.getMaxPartySize()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Максимум для цієї гри — " + game.getMaxPartySize() + " гравців");
            }
            maxMembers = dto.getMaxMembers();
        }

        validateTags(dto.getTags());
        Region region = parseEnum(Region.class, dto.getRegion(), "region");

        PartyRequest party = PartyRequest.builder()
                .creator(creator)
                .game(game)
                .maxMembers(maxMembers)
                .status(PartyStatus.OPEN)
                .isOpen(true)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .eventTime(dto.getEventTime())
                .platform(dto.getPlatform())
                .languages(dto.getLanguages())
                .tags(dto.getTags())
                .region(region)
                .skillLevel(skillLevel)
                .playStyle(playStyle)
                .build();

        partyRequestRepository.save(party);

        String langLabel = (dto.getLanguages() != null && !dto.getLanguages().isEmpty())
                ? String.join("/", dto.getLanguages()) : "International";
        String chatTitle = game.getName() + " • " + langLabel;
        Chat groupChat = chatService.createGroupChat(chatTitle, creator);
        party.setChat(groupChat);
        partyRequestRepository.save(party);

        PartyMember creatorMember = PartyMember.builder()
                .partyRequest(party)
                .user(creator)
                .build();
        partyMemberRepository.save(creatorMember);

        return toDTO(party, List.of(creatorMember));
    }

    @Transactional
    public PartyRequestDTO joinParty(User user, Long partyId) {

        if (partyMemberRepository.existsActivePartyForUser(user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "У вас вже є активне лобі");
        }

        PartyRequest party = getPartyWithStatus(partyId, PartyStatus.OPEN);

        if (partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ви вже в цьому лобі");
        }

        int currentCount = partyMemberRepository.countByPartyRequestId(partyId);
        if (currentCount >= party.getMaxMembers()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі заповнене");
        }

        PartyMember member = PartyMember.builder()
                .partyRequest(party)
                .user(user)
                .build();
        partyMemberRepository.save(member);

        if (party.getChat() != null) {
            chatService.addParticipant(party.getChat().getId(), user);
        }

        currentCount++;
        if (currentCount >= party.getMaxMembers()) {
            party.setStatus(PartyStatus.FULL);
            party.setIsOpen(false);
            partyRequestRepository.save(party);
        }

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO leaveParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!ACTIVE_STATUSES.contains(party.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі вже неактивне");
        }

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ви не в цьому лобі");
        }

        partyMemberRepository.deleteByPartyRequestIdAndUserId(partyId, user.getId());

        if (party.getChat() != null) {
            chatService.removeParticipant(party.getChat().getId(), user);
        }

        List<PartyMember> remaining = partyMemberRepository.findByPartyRequestIdOrderByJoinedAtAsc(partyId);

        if (remaining.isEmpty()) {
            party.setStatus(PartyStatus.CANCELLED);
            party.setIsOpen(false);
            partyRequestRepository.save(party);
            broadcastPartyUpdate(partyId);
            return toDTO(party, remaining);
        }

        if (party.getCreator().getId().equals(user.getId())) {
            PartyMember newLeader = remaining.get(0);
            party.setCreator(newLeader.getUser());
        }

        if (party.getStatus() == PartyStatus.FULL && remaining.size() < party.getMaxMembers()) {
            party.setStatus(PartyStatus.OPEN);
            party.setIsOpen(true);
        }

        partyRequestRepository.save(party);

        PartyRequestDTO result = toDTO(party, remaining);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO kickMember(User creator, Long partyId, Long userId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!ACTIVE_STATUSES.contains(party.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі вже неактивне");
        }

        if (!party.getCreator().getId().equals(creator.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може кікати гравців");
        }

        if (creator.getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Не можна кікнути себе");
        }

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Гравець не в цьому лобі");
        }

        partyMemberRepository.deleteByPartyRequestIdAndUserId(partyId, userId);

        if (party.getChat() != null) {
            try {
                chatService.removeParticipantById(party.getChat().getId(), userId);
            } catch (Exception ignored) {}
        }

        if (party.getStatus() == PartyStatus.FULL) {
            party.setStatus(PartyStatus.OPEN);
            party.setIsOpen(true);
            partyRequestRepository.save(party);
        }

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO closeParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може закрити лобі");
        }

        if (!ACTIVE_STATUSES.contains(party.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі вже неактивне");
        }

        party.setStatus(PartyStatus.CANCELLED);
        party.setIsOpen(false);
        partyRequestRepository.save(party);

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO startGame(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може почати гру");
        }

        if (party.getStatus() != PartyStatus.OPEN && party.getStatus() != PartyStatus.FULL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі має бути відкритим або заповненим для старту");
        }

        party.setStatus(PartyStatus.IN_GAME);
        party.setIsOpen(false);
        party.setStartedAt(Instant.now());
        partyRequestRepository.save(party);

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    @Transactional
    public PartyRequestDTO completeParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Тільки хост може завершити гру");
        }

        if (party.getStatus() != PartyStatus.IN_GAME) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Лобі має бути в стані IN_GAME");
        }

        party.setStatus(PartyStatus.COMPLETED);
        party.setIsOpen(false);
        party.setCompletedAt(Instant.now());
        partyRequestRepository.save(party);

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        PartyRequestDTO result = toDTO(party, members);
        broadcastPartyUpdate(partyId);
        return result;
    }

    public List<PartyRequestDTO> getOpenParties(Long gameId, String platform,
                                                String skillLevel, String playStyle) {

        String platformParam = (platform != null && !platform.isBlank()) ? platform : null;

        List<PartyRequest> parties = partyRequestRepository.findOpenWithFilters(
                gameId, skillLevel, playStyle, platformParam
        );

        return parties.stream()
                .map(p -> {
                    int count = partyMemberRepository.countByPartyRequestId(p.getId());
                    return toListDTO(p, count);
                })
                .toList();
    }

    public PartyRequestDTO getPartyById(Long partyId) {
        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        return toDTO(party, members);
    }

    public UserStatsDTO getUserStats(Long userId) {
        long completed = partyMemberRepository.countCompletedPartiesByUserId(userId);
        long created = partyMemberRepository.countCompletedPartiesCreatedByUserId(userId);
        long playTimeSeconds = partyMemberRepository.sumPlayTimeSecondsByUserId(userId);
        double hoursPlayed = Math.round(playTimeSeconds / 36.0) / 100.0;

        String favGameName = null;
        String favGameImageUrl = null;
        Long favGameId = null;
        var favGameIdOpt = partyMemberRepository.findFavoriteGameIdByUserId(userId);
        if (favGameIdOpt.isPresent()) {
            var gameOpt = gameRepository.findById(favGameIdOpt.get());
            if (gameOpt.isPresent()) {
                favGameId = gameOpt.get().getId();
                favGameName = gameOpt.get().getName();
                favGameImageUrl = gameOpt.get().getImageUrl();
            }
        }

        return UserStatsDTO.builder()
                .completedGames(completed)
                .partiesCreated(created)
                .partiesJoined(completed - created)
                .hoursPlayed(hoursPlayed)
                .favoriteGameName(favGameName)
                .favoriteGameImageUrl(favGameImageUrl)
                .favoriteGameId(favGameId)
                .build();
    }

    public Page<PartyRequestDTO> getOpenPartiesPaged(Long gameId, String platform,
                                                     String skillLevel, String playStyle,
                                                     String q, Pageable pageable) {

        String platformParam = (platform != null && !platform.isBlank()) ? platform : null;
        String qParam = (q != null && !q.isBlank()) ? q.trim() : null;

        Page<PartyRequest> page = partyRequestRepository.findOpenWithFiltersPaged(
                gameId, skillLevel, playStyle, platformParam, qParam, pageable
        );

        return page.map(p -> {
            int count = partyMemberRepository.countByPartyRequestId(p.getId());
            return toListDTO(p, count);
        });
    }

    public List<PartyRequestDTO> getMyParties(User user) {
        return partyMemberRepository.findActivePartiesByUserId(user.getId()).stream()
                .map(pm -> {
                    PartyRequest party = pm.getPartyRequest();
                    int count = partyMemberRepository.countByPartyRequestId(party.getId());
                    return toListDTO(party, count);
                })
                .toList();
    }

    public Page<PartyRequestDTO> getPartyHistory(User user, Pageable pageable) {
        Page<PartyRequest> page = partyRequestRepository.findHistoryByUserId(user.getId(), pageable);
        return page.map(p -> {
            int count = partyMemberRepository.countByPartyRequestId(p.getId());
            return toListDTO(p, count);
        });
    }

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void autoExpireParties() {
        Instant openCutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        partyRequestRepository.cancelStaleOpenParties(openCutoff);

        Instant inGameCutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        partyRequestRepository.completeStaleInGameParties(inGameCutoff);
    }

    private PartyRequest getPartyWithStatus(Long partyId, PartyStatus requiredStatus) {
        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));
        if (party.getStatus() != requiredStatus) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Лобі має бути у статусі " + requiredStatus + ", поточний: " + party.getStatus());
        }
        return party;
    }

    private PartyRequestDTO toDTO(PartyRequest party, List<PartyMember> members) {
        String creatorAvatarUrl = profileRepository.findByUserId(party.getCreator().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        List<PartyMemberDTO> memberDTOs = members.stream()
                .map(m -> {
                    String avatarUrl = profileRepository.findByUserId(m.getUser().getId())
                            .map(p -> p.getAvatarUrl())
                            .orElse(null);
                    return PartyMemberDTO.builder()
                            .userId(m.getUser().getId())
                            .displayName(m.getUser().getDisplayName())
                            .avatarUrl(avatarUrl)
                            .creator(m.getUser().getId().equals(party.getCreator().getId()))
                            .joinedAt(m.getJoinedAt())
                            .build();
                })
                .toList();

        return PartyRequestDTO.builder()
                .id(party.getId())
                .creatorId(party.getCreator().getId())
                .creatorDisplayName(party.getCreator().getDisplayName())
                .creatorAvatarUrl(creatorAvatarUrl)
                .gameId(party.getGame().getId())
                .gameName(party.getGame().getName())
                .gameImageUrl(party.getGame().getImageUrl())
                .maxMembers(party.getMaxMembers())
                .currentMembers(memberDTOs.size())
                .isOpen(party.getStatus() == PartyStatus.OPEN)
                .status(party.getStatus().name())
                .title(party.getTitle())
                .description(party.getDescription())
                .eventTime(party.getEventTime())
                .platform(party.getPlatform())
                .languages(party.getLanguages())
                .skillLevel(party.getSkillLevel() != null ? party.getSkillLevel().name() : null)
                .playStyle(party.getPlayStyle() != null ? party.getPlayStyle().name() : null)
                .tags(party.getTags())
                .region(party.getRegion() != null ? party.getRegion().name() : null)
                .members(memberDTOs)
                .chatId(party.getChat() != null ? party.getChat().getId() : null)
                .createdAt(party.getCreatedAt())
                .build();
    }

    private PartyRequestDTO toListDTO(PartyRequest party, int currentMembers) {
        String creatorAvatarUrl = profileRepository.findByUserId(party.getCreator().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        return PartyRequestDTO.builder()
                .id(party.getId())
                .creatorId(party.getCreator().getId())
                .creatorDisplayName(party.getCreator().getDisplayName())
                .creatorAvatarUrl(creatorAvatarUrl)
                .gameId(party.getGame().getId())
                .gameName(party.getGame().getName())
                .gameImageUrl(party.getGame().getImageUrl())
                .maxMembers(party.getMaxMembers())
                .currentMembers(currentMembers)
                .isOpen(party.getStatus() == PartyStatus.OPEN)
                .status(party.getStatus().name())
                .title(party.getTitle())
                .description(party.getDescription())
                .eventTime(party.getEventTime())
                .platform(party.getPlatform())
                .languages(party.getLanguages())
                .skillLevel(party.getSkillLevel() != null ? party.getSkillLevel().name() : null)
                .playStyle(party.getPlayStyle() != null ? party.getPlayStyle().name() : null)
                .tags(party.getTags())
                .region(party.getRegion() != null ? party.getRegion().name() : null)
                .members(null)
                .chatId(party.getChat() != null ? party.getChat().getId() : null)
                .createdAt(party.getCreatedAt())
                .build();
    }

    private void validatePlatforms(List<String> platforms) {
        if (platforms == null || platforms.isEmpty()) return;
        for (String p : platforms) {
            try {
                Platform.valueOf(p.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid platform: " + p + ". Allowed: " + java.util.Arrays.toString(Platform.values()));
            }
        }
    }

    private void validateLanguages(List<String> languages) {
        if (languages == null || languages.isEmpty()) return;
        for (String lang : languages) {
            try {
                Language.valueOf(lang.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid language: " + lang + ". Allowed: " + java.util.Arrays.toString(Language.values()));
            }
        }
    }

    private void validateTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        if (tags.size() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Максимум 5 тегів");
        }
        for (String tag : tags) {
            if (tag.length() > 30) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Тег занадто довгий (макс 30 символів)");
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName + ": " + value);
        }
    }

    public void broadcastPartyUpdate(Long partyId) {
        try {
            PartyRequest party = partyRequestRepository.findById(partyId).orElse(null);
            if (party == null) return;
            List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
            PartyRequestDTO dto = toDTO(party, members);
            messagingTemplate.convertAndSend("/topic/party/" + partyId, dto);
        } catch (Exception ignored) {
        }
    }
}