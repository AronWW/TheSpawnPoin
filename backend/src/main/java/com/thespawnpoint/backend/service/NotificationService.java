package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.NotificationDTO;
import com.thespawnpoint.backend.entity.social.Notification;
import com.thespawnpoint.backend.entity.social.NotificationType;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void send(User recipient, NotificationType type, String message) {
        send(recipient, type, message, null);
    }

    @Transactional
    public void send(User recipient, NotificationType type, String message, Long referenceId) {
        Notification notification = notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .build());

        NotificationDTO dto = toDTO(notification);

        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/notifications",
                dto
        );

        broadcastUnreadCount(recipient);
    }

    @Transactional
    public void updateExistingOrSend(User recipient, NotificationType type, String newMessage, Long referenceId) {
        var existing = notificationRepository
                .findFirstByUserIdAndTypeAndReferenceIdOrderByCreatedAtDesc(
                        recipient.getId(), type, referenceId);

        if (existing.isPresent()) {
            Notification notification = existing.get();
            notification.setMessage(newMessage);
            notification.setIsRead(false);
            notificationRepository.save(notification);

            NotificationDTO dto = toDTO(notification);
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/notifications",
                    dto
            );
            broadcastUnreadCount(recipient);
        } else {
            send(recipient, type, newMessage, referenceId);
        }
    }

    public Page<NotificationDTO> getUserNotifications(User user, int page, int size) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(this::toDTO);
    }

    public int getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndIsRead(user.getId(), false);
    }

    @Transactional
    public void markAsRead(User user, Long notificationId) {
        Notification notification = getOwnedNotification(user, notificationId);
        notification.setIsRead(true);
        notificationRepository.save(notification);
        broadcastUnreadCount(user);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user.getId());
        broadcastUnreadCount(user);
    }

    @Transactional
    public void deleteNotification(User user, Long notificationId) {
        Notification notification = getOwnedNotification(user, notificationId);
        notificationRepository.delete(notification);
        broadcastUnreadCount(user);
    }

    @Transactional
    public void deleteAllNotifications(User user) {
        notificationRepository.deleteAllByUserId(user.getId());
        broadcastUnreadCount(user);
    }

    private Notification getOwnedNotification(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
        }

        return notification;
    }

    private void broadcastUnreadCount(User user) {
        int count = notificationRepository.countByUserIdAndIsRead(user.getId(), false);
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications/unread-count",
                Map.of("count", count)
        );
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType().name())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .read(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

