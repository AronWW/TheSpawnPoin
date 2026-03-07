package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.ChatDTO;
import com.thespawnpoint.backend.dto.ChatParticipantDTO;
import com.thespawnpoint.backend.dto.MessageDTO;
import com.thespawnpoint.backend.entity.chat.*;
import com.thespawnpoint.backend.entity.party.PartyRequest;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PartyRequestRepository partyRequestRepository;
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

    @Transactional
    public MessageDTO sendMessage(User sender, String recipientEmail, String content) {
        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));

        if (sender.getId().equals(recipient.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot send message to yourself");
        }

        if (recipient.getRole() == com.thespawnpoint.backend.entity.user.Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Recipient not found");
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
                .collect(Collectors.toList());

        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public Chat createGroupChat(String title, User creator) {
        return createGroupChat(title, creator, true);
    }

    @Transactional
    public Chat createGroupChat(String title, User creator, boolean partyLinked) {
        Chat chat = chatRepository.save(Chat.builder()
                .isGroup(true)
                .partyLinked(partyLinked)
                .title(title)
                .build());

        chatParticipantRepository.save(ChatParticipant.builder()
                .chat(chat).user(creator).build());

        return chat;
    }

    @Transactional
    public ChatDTO createStandaloneGroupChat(User creator, String title, List<String> memberEmails) {
        Chat chat = createGroupChat(title, creator, false);

        sendSystemMessage(chat, creator.getDisplayName() + " created the group");

        for (String email : memberEmails) {
            User member = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + email));

            if (member.getId().equals(creator.getId())) {
                continue;
            }

            chatParticipantRepository.save(ChatParticipant.builder()
                    .chat(chat).user(member).build());

            sendSystemMessage(chat, member.getDisplayName() + " joined the chat");
        }

        return buildChatDTO(chat, creator);
    }

    @Transactional
    public void addGroupChatParticipant(User requester, Long chatId, String memberEmail) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chat.getIsGroup() || chat.getPartyLinked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot add members to this chat");
        }

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, requester.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (chatParticipantRepository.existsByChatIdAndUserId(chatId, member.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "User is already a participant");
        }

        chatParticipantRepository.save(ChatParticipant.builder()
                .chat(chat).user(member).build());

        sendSystemMessage(chat, member.getDisplayName() + " joined the chat");
    }

    @Transactional
    public void leaveGroupChat(User user, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chat.getIsGroup() || chat.getPartyLinked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot leave this chat via this endpoint");
        }

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You are not a participant of this chat");
        }

        sendSystemMessage(chat, user.getDisplayName() + " left the chat");
        chatParticipantRepository.deleteByChatIdAndUserId(chatId, user.getId());

        int remaining = chatParticipantRepository.countByChatId(chatId);
        if (remaining == 0) {
            chatRepository.deleteById(chatId);
        }
    }

    @Transactional
    public ChatDTO renameGroupChat(User requester, Long chatId, String newTitle) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chat.getIsGroup() || chat.getPartyLinked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot rename this chat");
        }

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, requester.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        chat.setTitle(newTitle);
        chatRepository.save(chat);

        sendSystemMessage(chat, requester.getDisplayName() + " renamed the chat to \"" + newTitle + "\"");

        return buildChatDTO(chat, requester);
    }

    @Transactional
    public void addParticipant(Long chatId, User user) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            return;
        }

        chatParticipantRepository.save(ChatParticipant.builder()
                .chat(chat).user(user).build());

        sendSystemMessage(chat, user.getDisplayName() + " joined the party");
    }

    @Transactional
    public void removeParticipant(Long chatId, User user) {
        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            return;
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
        sendSystemMessage(chat, user.getDisplayName() + " left the party");

        chatParticipantRepository.deleteByChatIdAndUserId(chatId, user.getId());
    }

    @Transactional
    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
    }

    @Transactional
    public MessageDTO sendGroupMessage(User sender, Long chatId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chat.getIsGroup()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This is not a group chat");
        }

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, sender.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        Message saved = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .build());

        MessageDTO dto = toDTO(saved);

        List<ChatParticipant> participants = chatParticipantRepository.findByChatId(chatId);
        for (ChatParticipant cp : participants) {
            messagingTemplate.convertAndSendToUser(
                    cp.getUser().getEmail(), "/queue/messages", dto);
        }

        return dto;
    }

    @Transactional
    public List<MessageDTO> getGroupHistory(User user, Long chatId, int page, int size) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        if (page == 0) {
            self().markGroupAsRead(user, chatId);
        }

        List<MessageDTO> messages = messageRepository
                .findByChatOrderBySentAtDesc(chat, PageRequest.of(page, size))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public void markGroupAsRead(User reader, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        messageRepository.markAsReadInChat(chat, reader);

        List<ChatParticipant> participants = chatParticipantRepository.findByChatId(chatId);
        for (ChatParticipant cp : participants) {
            if (!cp.getUser().getId().equals(reader.getId())) {
                messagingTemplate.convertAndSendToUser(
                        cp.getUser().getEmail(),
                        "/queue/read",
                        Map.of("readerEmail", reader.getEmail(), "chatId", chatId)
                );
            }
        }
    }

    public void sendGroupTypingIndicator(User sender, Long chatId) {
        java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
        payload.put("senderEmail", sender.getEmail());
        payload.put("chatId", chatId);

        List<ChatParticipant> participants = chatParticipantRepository.findByChatId(chatId);
        for (ChatParticipant cp : participants) {
            if (!cp.getUser().getId().equals(sender.getId())) {
                messagingTemplate.convertAndSendToUser(
                        cp.getUser().getEmail(),
                        "/queue/typing",
                        payload
                );
            }
        }
    }

    private void sendSystemMessage(Chat chat, String content) {
        Message saved = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(null)
                .content(content)
                .system(true)
                .build());

        MessageDTO dto = toDTO(saved);

        List<ChatParticipant> participants = chatParticipantRepository.findByChatId(chat.getId());
        for (ChatParticipant cp : participants) {
            messagingTemplate.convertAndSendToUser(
                    cp.getUser().getEmail(), "/queue/messages", dto);
        }
    }

    @Transactional
    public List<ChatDTO> getUserChats(User currentUser) {
        return chatRepository.findAllByUser(currentUser).stream()
                .map(chat -> buildChatDTO(chat, currentUser))
                .toList();
    }

    private ChatDTO buildChatDTO(Chat chat, User currentUser) {
        String lastMessage = null;
        java.time.Instant lastMessageAt = null;
        var lastMsg = messageRepository.findFirstByChatOrderBySentAtDesc(chat);
        if (lastMsg.isPresent()) {
            lastMessage = lastMsg.get().getContent();
            lastMessageAt = lastMsg.get().getSentAt();
        }

        int unread = messageRepository.countUnreadInChat(chat, currentUser);

        if (Boolean.TRUE.equals(chat.getIsGroup())) {
            return buildGroupChatDTO(chat, currentUser, lastMessage, lastMessageAt, unread);
        } else {
            return buildDmChatDTO(chat, currentUser, lastMessage, lastMessageAt, unread);
        }
    }

    private ChatDTO buildDmChatDTO(Chat chat, User currentUser,
                                    String lastMessage, java.time.Instant lastMessageAt, int unread) {
        User partner = chatParticipantRepository.findByChatId(chat.getId()).stream()
                .map(ChatParticipant::getUser)
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(currentUser);

        return ChatDTO.builder()
                .id(chat.getId())
                .group(false)
                .partnerEmail(partner.getEmail())
                .partnerDisplayName(partner.getDisplayName())
                .partnerStatus(partner.getStatus().name())
                .partnerLastSeen(partner.getLastSeen() != null ? partner.getLastSeen().toString() : null)
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unread)
                .build();
    }

    private ChatDTO buildGroupChatDTO(Chat chat, User currentUser,
                                       String lastMessage, java.time.Instant lastMessageAt, int unread) {
        List<ChatParticipantDTO> participants = chatParticipantRepository.findByChatId(chat.getId()).stream()
                .map(cp -> {
                    String avatarUrl = profileRepository.findByUserId(cp.getUser().getId())
                            .map(p -> p.getAvatarUrl())
                            .orElse(null);
                    return ChatParticipantDTO.builder()
                            .userId(cp.getUser().getId())
                            .displayName(cp.getUser().getDisplayName())
                            .email(cp.getUser().getEmail())
                            .avatarUrl(avatarUrl)
                            .build();
                })
                .toList();

        Long partyId = partyRequestRepository.findByChatId(chat.getId())
                .map(PartyRequest::getId)
                .orElse(null);

        return ChatDTO.builder()
                .id(chat.getId())
                .group(true)
                .partyLinkedFlag(Boolean.TRUE.equals(chat.getPartyLinked()))
                .title(chat.getTitle())
                .partyId(partyId)
                .participants(participants)
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
        String senderEmail = null;
        String senderName = null;
        String senderAvatarUrl = null;

        if (m.getSender() != null) {
            senderEmail = m.getSender().getEmail();
            senderName = m.getSender().getDisplayName();
            senderAvatarUrl = profileRepository.findByUserId(m.getSender().getId())
                    .map(p -> p.getAvatarUrl())
                    .orElse(null);
        }

        return MessageDTO.builder()
                .id(m.getId())
                .chatId(m.getChat().getId())
                .senderEmail(senderEmail)
                .senderName(senderName)
                .senderAvatarUrl(senderAvatarUrl)
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .read(m.isRead())
                .system(m.isSystem())
                .build();
    }
}
