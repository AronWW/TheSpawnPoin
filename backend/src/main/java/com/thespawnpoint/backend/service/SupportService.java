package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.CreateTicketDTO;
import com.thespawnpoint.backend.dto.TicketDTO;
import com.thespawnpoint.backend.dto.TicketMessageDTO;
import com.thespawnpoint.backend.entity.support.SupportMessage;
import com.thespawnpoint.backend.entity.support.SupportTicket;
import com.thespawnpoint.backend.entity.support.TicketStatus;
import com.thespawnpoint.backend.entity.social.NotificationType;
import com.thespawnpoint.backend.entity.user.Role;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.SupportMessageRepository;
import com.thespawnpoint.backend.repository.SupportTicketRepository;
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
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    @Transactional
    public TicketDTO createTicket(User user, CreateTicketDTO dto) {
        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(dto.getSubject())
                .build();
        ticketRepository.save(ticket);

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .sender(user)
                .content(dto.getMessage())
                .build();
        messageRepository.save(message);

        return toDTO(ticket);
    }

    public List<TicketDTO> getMyTickets(User user) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TicketMessageDTO> getTicketMessages(Long ticketId, User user) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Тікет не знайдено"));

        if (user.getRole() != Role.ADMIN && !ticket.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Доступ заборонено");
        }

        return messageRepository.findByTicketIdOrderBySentAtAsc(ticketId)
                .stream()
                .map(this::toMessageDTO)
                .toList();
    }

    @Transactional
    public TicketMessageDTO replyToTicket(Long ticketId, User sender, String content) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Тікет не знайдено"));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Тікет закрито");
        }

        if (sender.getRole() != Role.ADMIN && !ticket.getUser().getId().equals(sender.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Доступ заборонено");
        }

        if (sender.getRole() == Role.ADMIN && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        SupportMessage message = SupportMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .content(content)
                .build();
        messageRepository.save(message);

        if (sender.getRole() == Role.ADMIN) {
            notificationService.send(
                    ticket.getUser(),
                    NotificationType.SUPPORT_REPLY,
                    "Підтримка відповіла на ваш тікет: \"" + ticket.getSubject() + "\"",
                    ticket.getId()
            );
        }

        return toMessageDTO(message);
    }

    public Page<TicketDTO> getAllTickets(String statusFilter, Pageable pageable) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            TicketStatus status;
            try {
                status = TicketStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Невідомий статус: " + statusFilter);
            }
            return ticketRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::toDTO);
        }
        return ticketRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    @Transactional
    public TicketDTO changeTicketStatus(Long ticketId, String newStatusStr) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Тікет не знайдено"));

        TicketStatus newStatus;
        try {
            newStatus = TicketStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Невідомий статус: " + newStatusStr);
        }

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
            notificationService.send(
                    ticket.getUser(),
                    NotificationType.SUPPORT_REPLY,
                    "Ваш тікет \"" + ticket.getSubject() + "\" закрито.",
                    ticket.getId()
            );
        }
        ticketRepository.save(ticket);
        return toDTO(ticket);
    }

    private TicketDTO toDTO(SupportTicket t) {
        return TicketDTO.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .userDisplayName(t.getUser().getDisplayName())
                .subject(t.getSubject())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .closedAt(t.getClosedAt())
                .build();
    }

    private TicketMessageDTO toMessageDTO(SupportMessage m) {
        String avatarUrl = profileRepository.findByUserId(m.getSender().getId())
                .map(p -> p.getAvatarUrl())
                .orElse(null);

        return TicketMessageDTO.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .senderDisplayName(m.getSender().getDisplayName())
                .senderAvatarUrl(avatarUrl)
                .admin(m.getSender().getRole() == Role.ADMIN)
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .build();
    }
}

