package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.NotificationDTO;
import com.thespawnpoint.backend.entity.social.Notification;
import com.thespawnpoint.backend.entity.social.NotificationType;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void send(User recipient, NotificationType type, String message) {
        Notification notification = notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(type)
                .message(message)
                .build());

        NotificationDTO dto = toDTO(notification);

        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/notifications",
                dto
        );
    }

    public List<NotificationDTO> getUserNotifications(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toDTO)
                .toList();
    }

    public int getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndIsRead(user.getId(), false);
    }

    @Transactional
    public void markAsRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user.getId());
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType().name())
                .message(n.getMessage())
                .read(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

