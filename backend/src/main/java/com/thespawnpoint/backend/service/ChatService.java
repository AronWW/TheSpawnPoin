package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.ChatDTO;
import com.thespawnpoint.backend.dto.MessageDTO;
import com.thespawnpoint.backend.entity.chat.*;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationContext applicationContext;

    private ChatService self() {
        return applicationContext.getBean(ChatService.class);
    }

    @Transactional
    public Chat getOrCreateDmChat(User user1, User user2) {
        return chatRepository.findDmChat(user1, user2).orElseGet(() -> {
            Chat chat = chatRepository.save(Chat.builder()
                    .isGroup(false)
                    .build());

            chatParticipantRepository.save(ChatParticipant.builder()
                    .chat(chat).user(user1).build());
            chatParticipantRepository.save(ChatParticipant.builder()
                    .chat(chat).user(user2).build());

            return chat;
        });
    }

    // Надіслати повідомлення

    @Transactional
    public MessageDTO sendMessage(User sender, String recipientEmail, String content) {
        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));

        if (sender.getId().equals(recipient.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot send message to yourself");
        }

        Chat chat = self().getOrCreateDmChat(sender, recipient);

        Message saved = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .build());

        MessageDTO dto = toDTO(saved);

        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", dto);

        return dto;
    }


    // Typing індикатор
    public void sendTypingIndicator(User sender, String recipientEmail) {
        Long chatId = getChatIdIfExists(sender, recipientEmail);
        java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
        payload.put("senderEmail", sender.getEmail());
        payload.put("chatId", chatId);

        messagingTemplate.convertAndSendToUser(
                recipientEmail,
                "/queue/typing",
                payload
        );
    }

    // Позначити повідомлення як прочитані та сповістити відправника
    @Transactional
    public void markAsReadAndNotify(User reader, String senderEmail) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        chatRepository.findDmChat(reader, sender).ifPresent(chat -> {
            messageRepository.markAsReadInChat(chat, reader);
            messagingTemplate.convertAndSendToUser(
                    sender.getEmail(),
                    "/queue/read",
                    Map.of("readerEmail", reader.getEmail(), "chatId", chat.getId())
            );
        });
    }

    // Список чатів юзера
    @Transactional
    public List<ChatDTO> getUserChats(User currentUser) {
        return chatRepository.findAllByUser(currentUser).stream()
                .map(chat -> buildChatDTO(chat, currentUser))
                .toList();
    }

    // Історія повідомлень
    @Transactional
    public List<MessageDTO> getHistory(User currentUser, String partnerEmail, int page, int size) {
        User partner = userRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Chat chat = chatRepository.findDmChat(currentUser, partner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (page == 0) {
            self().markAsReadAndNotify(currentUser, partner.getEmail());
        }

        List<MessageDTO> messages = messageRepository
                .findByChatOrderBySentAtDesc(chat, PageRequest.of(page, size))
                .stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());

        Collections.reverse(messages);
        return messages;
    }

    private ChatDTO buildChatDTO(Chat chat, User currentUser) {
        User partner = chatParticipantRepository.findByChatId(chat.getId()).stream()
                .map(ChatParticipant::getUser)
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(currentUser); // fallback якщо чомусь один учасник

        String lastMessage = null;
        java.time.Instant lastMessageAt = null;
        var lastMsg = messageRepository.findFirstByChatOrderBySentAtDesc(chat);
        if (lastMsg.isPresent()) {
            lastMessage = lastMsg.get().getContent();
            lastMessageAt = lastMsg.get().getSentAt();
        }

        int unread = messageRepository.countUnreadInChat(chat, currentUser);

        return ChatDTO.builder()
                .id(chat.getId())
                .partnerEmail(partner.getEmail())
                .partnerDisplayName(partner.getDisplayName())
                .partnerStatus(partner.getStatus().name())
                .partnerLastSeen(partner.getLastSeen() != null ? partner.getLastSeen().toString() : null)
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unread)
                .build();
    }

    private Long getChatIdIfExists(User sender, String recipientEmail) {
        return userRepository.findByEmail(recipientEmail)
                .flatMap(recipient -> chatRepository.findDmChat(sender, recipient))
                .map(Chat::getId)
                .orElse(null);
    }

    public MessageDTO toDTO(Message m) {
        return MessageDTO.builder()
                .id(m.getId())
                .chatId(m.getChat().getId())
                .senderEmail(m.getSender().getEmail())
                .senderName(m.getSender().getDisplayName())
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .read(m.isRead())
                .build();
    }
}
