package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.CreateReportDTO;
import com.thespawnpoint.backend.dto.ReportDTO;
import com.thespawnpoint.backend.dto.ReviewReportDTO;
import com.thespawnpoint.backend.entity.report.ReportReason;
import com.thespawnpoint.backend.entity.report.ReportStatus;
import com.thespawnpoint.backend.entity.report.UserReport;
import com.thespawnpoint.backend.entity.social.NotificationType;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.UserReportRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserReportRepository userReportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReportDTO createReport(User reporter, CreateReportDTO dto) {
        if (reporter.getId().equals(dto.getReportedUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Не можна поскаржитися на себе");
        }

        User reportedUser = userRepository.findById(dto.getReportedUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Користувача не знайдено"));

        if (reportedUser.getRole() == com.thespawnpoint.backend.entity.user.Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Користувача не знайдено");
        }

        ReportReason reason;
        try {
            reason = ReportReason.valueOf(dto.getReason().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Невідома причина скарги: " + dto.getReason());
        }

        UserReport report = UserReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reason(reason)
                .description(dto.getDescription())
                .build();

        userReportRepository.save(report);
        return toDTO(report);
    }

    public Page<ReportDTO> getReports(String statusFilter, Pageable pageable) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            ReportStatus status;
            try {
                status = ReportStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Невідомий статус: " + statusFilter);
            }
            return userReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::toDTO);
        }
        return userReportRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    @Transactional
    public ReportDTO reviewReport(Long reportId, ReviewReportDTO dto) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Скаргу не знайдено"));

        if (report.getStatus() != ReportStatus.OPEN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Скарга вже розглянута");
        }

        ReportStatus newStatus;
        try {
            newStatus = ReportStatus.valueOf(dto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Невідомий статус: " + dto.getStatus());
        }

        if (newStatus == ReportStatus.OPEN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Не можна повернути статус OPEN");
        }

        report.setStatus(newStatus);
        report.setAdminComment(dto.getAdminComment());
        report.setReviewedAt(Instant.now());
        userReportRepository.save(report);

        String statusLabel = newStatus == ReportStatus.REVIEWED ? "розглянуто" : "відхилено";
        String msg = "Вашу скаргу на " + report.getReportedUser().getDisplayName() + " " + statusLabel + ".";
        if (dto.getAdminComment() != null && !dto.getAdminComment().isBlank()) {
            msg += " Коментар: " + dto.getAdminComment();
        }
        notificationService.send(
                report.getReporter(),
                NotificationType.REPORT_REVIEWED,
                msg,
                report.getId()
        );

        return toDTO(report);
    }

    private ReportDTO toDTO(UserReport r) {
        return ReportDTO.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterDisplayName(r.getReporter().getDisplayName())
                .reportedUserId(r.getReportedUser().getId())
                .reportedUserDisplayName(r.getReportedUser().getDisplayName())
                .reason(r.getReason().name())
                .description(r.getDescription())
                .status(r.getStatus().name())
                .adminComment(r.getAdminComment())
                .createdAt(r.getCreatedAt())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}

