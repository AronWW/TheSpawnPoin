package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.CreatePartyRequestDTO;
import com.thespawnpoint.backend.dto.PartyMemberDTO;
import com.thespawnpoint.backend.dto.PartyRequestDTO;
import com.thespawnpoint.backend.entity.game.Game;
import com.thespawnpoint.backend.entity.party.PartyMember;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.user.PlayStyle;
import com.thespawnpoint.backend.entity.user.Platform;
import com.thespawnpoint.backend.entity.user.SkillLevel;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.entity.chat.Chat;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRequestRepository partyRequestRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final GameRepository gameRepository;
    private final ProfileRepository profileRepository;
    private final ChatService chatService;

    @Transactional
    public PartyRequestDTO createParty(User creator, CreatePartyRequestDTO dto) {

        if (partyMemberRepository.existsActivePartyForUser(creator.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an active party");
        }

        Game game = gameRepository.findById(dto.getGameId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Game not found"));

        validatePlatforms(dto.getPlatform());
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

        PartyRequest party = PartyRequest.builder()
                .creator(creator)
                .game(game)
                .maxMembers(maxMembers)
                .description(dto.getDescription())
                .eventTime(dto.getEventTime())
                .platform(dto.getPlatform())
                .language(dto.getLanguage())
                .skillLevel(skillLevel)
                .playStyle(playStyle)
                .build();

        partyRequestRepository.save(party);

        String chatTitle = game.getName() + " • " + (dto.getLanguage() != null ? dto.getLanguage() : "International");
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
            throw new ApiException(HttpStatus.CONFLICT, "You already have an active party");
        }

        PartyRequest party = getOpenPartyOrThrow(partyId);

        if (partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already in this party");
        }

        int currentCount = partyMemberRepository.countByPartyRequestId(partyId);
        if (currentCount >= party.getMaxMembers()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party is full");
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
            party.setIsOpen(false);
            partyRequestRepository.save(party);
        }

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        return toDTO(party, members);
    }

    @Transactional
    public PartyRequestDTO leaveParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You are not in this party");
        }

        partyMemberRepository.deleteByPartyRequestIdAndUserId(partyId, user.getId());

        if (party.getChat() != null) {
            chatService.removeParticipant(party.getChat().getId(), user);
        }

        List<PartyMember> remaining = partyMemberRepository.findByPartyRequestIdOrderByJoinedAtAsc(partyId);

        if (remaining.isEmpty()) {
            Long chatId = party.getChat() != null ? party.getChat().getId() : null;
            party.setChat(null);
            partyRequestRepository.save(party);
            if (chatId != null) {
                chatService.deleteChat(chatId);
            }
            partyRequestRepository.delete(party);
            return null;
        }

        if (party.getCreator().getId().equals(user.getId())) {
            PartyMember newLeader = remaining.get(0);
            party.setCreator(newLeader.getUser());
            partyRequestRepository.save(party);
        }

        if (!party.getIsOpen() && remaining.size() < party.getMaxMembers()) {
            party.setIsOpen(true);
            partyRequestRepository.save(party);
        }

        return toDTO(party, remaining);
    }

    @Transactional
    public PartyRequestDTO closeParty(User user, Long partyId) {

        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!party.getCreator().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the party creator can close the party");
        }

        if (!party.getIsOpen()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party is already closed");
        }

        party.setIsOpen(false);
        partyRequestRepository.save(party);

        if (party.getChat() != null) {
            Long chatId = party.getChat().getId();
            party.setChat(null);
            partyRequestRepository.save(party);
            chatService.deleteChat(chatId);
        }

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(partyId);
        return toDTO(party, members);
    }

    public List<PartyRequestDTO> getOpenParties(Long gameId, String platform,
                                                String skillLevel, String playStyle,
                                                String language) {

        String platformParam = (platform != null && !platform.isBlank()) ? platform : null;

        List<PartyRequest> parties = partyRequestRepository.findOpenWithFilters(
                gameId, skillLevel, playStyle, language, platformParam
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

    public Page<PartyRequestDTO> getOpenPartiesPaged(Long gameId, String platform,
                                                     String skillLevel, String playStyle,
                                                     String language, Pageable pageable) {

        String platformParam = (platform != null && !platform.isBlank()) ? platform : null;

        Page<PartyRequest> page = partyRequestRepository.findOpenWithFiltersPaged(
                gameId, skillLevel, playStyle, language, platformParam, pageable
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

    private PartyRequest getOpenPartyOrThrow(Long partyId) {
        PartyRequest party = partyRequestRepository.findById(partyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));
        if (!party.getIsOpen()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Party is closed");
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
                .isOpen(party.getIsOpen())
                .description(party.getDescription())
                .eventTime(party.getEventTime())
                .platform(party.getPlatform())
                .language(party.getLanguage())
                .skillLevel(party.getSkillLevel() != null ? party.getSkillLevel().name() : null)
                .playStyle(party.getPlayStyle() != null ? party.getPlayStyle().name() : null)
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
                .isOpen(party.getIsOpen())
                .description(party.getDescription())
                .eventTime(party.getEventTime())
                .platform(party.getPlatform())
                .language(party.getLanguage())
                .skillLevel(party.getSkillLevel() != null ? party.getSkillLevel().name() : null)
                .playStyle(party.getPlayStyle() != null ? party.getPlayStyle().name() : null)
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

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid " + fieldName + ": " + value);
        }
    }
}