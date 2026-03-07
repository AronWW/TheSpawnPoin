package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.ReviewUnbanRequestDTO;
import com.thespawnpoint.backend.dto.UnbanRequestDTO;
import com.thespawnpoint.backend.entity.unban.UnbanRequest;
import com.thespawnpoint.backend.entity.unban.UnbanRequestStatus;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UnbanRequestRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UnbanRequestService {

    private final UnbanRequestRepository unbanRequestRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public UnbanRequestDTO createRequest(User user, String reason) {
        if (!user.isBanned()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ви не забанені");
        }

        if (unbanRequestRepository.existsByUserIdAndStatus(user.getId(), UnbanRequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "У вас вже є активний запит на розбан");
        }

        UnbanRequest request = UnbanRequest.builder()
                .user(user)
                .reason(reason)
                .banReason(user.getBanReason())
                .build();

        unbanRequestRepository.save(request);
        return toDTO(request);
    }

    public UnbanRequestDTO getMyLatestRequest(User user) {
        return unbanRequestRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(this::toDTO)
                .orElse(null);
    }

    public List<UnbanRequestDTO> getMyRequests(User user) {
        return unbanRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDTO).toList();
    }

    public Page<UnbanRequestDTO> getAll(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            UnbanRequestStatus s = UnbanRequestStatus.valueOf(status.toUpperCase());
            return unbanRequestRepository.findByStatusOrderByCreatedAtDesc(s, pageable).map(this::toDTO);
        }
        return unbanRequestRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    @Transactional
    public UnbanRequestDTO reviewRequest(Long id, ReviewUnbanRequestDTO dto) {
        UnbanRequest request = unbanRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Запит не знайдено"));

        if (request.getStatus() != UnbanRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Цей запит вже розглянуто");
        }

        UnbanRequestStatus newStatus = UnbanRequestStatus.valueOf(dto.getStatus().toUpperCase());
        if (newStatus == UnbanRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Некоректний статус");
        }

        request.setStatus(newStatus);
        request.setAdminComment(dto.getAdminComment());
        request.setReviewedAt(Instant.now());
        unbanRequestRepository.save(request);

        if (newStatus == UnbanRequestStatus.APPROVED) {
            User user = request.getUser();
            user.setBanned(false);
            user.setBanReason(null);
            user.setBannedAt(null);
            userRepository.save(user);
        }

        return toDTO(request);
    }

    public long countPending() {
        return unbanRequestRepository.countByStatus(UnbanRequestStatus.PENDING);
    }

    private UnbanRequestDTO toDTO(UnbanRequest r) {
        User u = r.getUser();
        String avatarUrl = profileRepository.findByUserId(u.getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        List<UnbanRequestDTO.BanRecordDTO> history = unbanRequestRepository
                .findByUserIdOrderByCreatedAtDesc(u.getId())
                .stream()
                .map(req -> UnbanRequestDTO.BanRecordDTO.builder()
                        .banReason(req.getBanReason())
                        .createdAt(req.getCreatedAt())
                        .requestReason(req.getReason())
                        .requestStatus(req.getStatus().name())
                        .build())
                .toList();

        return UnbanRequestDTO.builder()
                .id(r.getId())
                .userId(u.getId())
                .userDisplayName(u.getDisplayName())
                .userEmail(u.getEmail())
                .userAvatarUrl(avatarUrl)
                .reason(r.getReason())
                .status(r.getStatus().name())
                .adminComment(r.getAdminComment())
                .banReason(r.getBanReason())
                .banHistory(history)
                .createdAt(r.getCreatedAt())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}

