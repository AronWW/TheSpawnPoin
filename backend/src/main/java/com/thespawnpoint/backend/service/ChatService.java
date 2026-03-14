package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.*;
import com.thespawnpoint.backend.entity.chat.*;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
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
    public MessageDTO sendMessage(User sender, String recipientEmail, String content, Long replyToId) {
        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));

        if (sender.getId().equals(recipient.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot send message to yourself");
        }

        if (recipient.getRole() == com.thespawnpoint.backend.entity.user.Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Recipient not found");
        }

        Chat chat = self().getOrCreateDmChat(sender, recipient);

        chatParticipantRepository.findByChatIdAndUserId(chat.getId(), recipient.getId())
                .ifPresent(cp -> {
                    if (cp.getDeletedAt() != null) {
                        cp.setDeletedAt(null);
                        chatParticipantRepository.save(cp);
                    }
                });

        Message replyTo = resolveReplyTo(replyToId, chat);

        Message saved = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .replyTo(replyTo)
                .build());

        MessageDTO dto = toDTO(saved);

        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", dto);

        return dto;
    }

    public void sendTypingIndicator(User sender, String recipientEmail) {
        Long chatId = getChatIdIfExists(sender, recipientEmail);
        HashMap<String, Object> payload = new HashMap<>();
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

            List<String> emails = chatParticipantRepository.findByChatId(chatId).stream()
                    .map(cp -> cp.getUser().getEmail())
                    .collect(Collectors.toList());

            partyRequestRepository.findByChatId(chatId).ifPresent(party -> {
                party.setChat(null);
                partyRequestRepository.save(party);
            });

            chatRepository.deleteById(chatId);

            ChatEventDTO event = new ChatEventDTO("CHAT_DELETED", chatId, Map.of("chatId", chatId));
            for (String email : emails) {
                messagingTemplate.convertAndSendToUser(email, "/queue/chat-events", event);
            }
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

        chatParticipantRepository.deleteByChatIdAndUserId(chatId, user.getId());

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
        sendSystemMessage(chat, user.getDisplayName() + " left the party");
    }

    @Transactional
    public void removeParticipantById(Long chatId, Long userId) {
        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, userId)) {
            return;
        }

        chatParticipantRepository.deleteByChatIdAndUserId(chatId, userId);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
        sendSystemMessage(chat, "A member was removed from the party");
    }

    @Transactional
    public void deleteChat(Long chatId) {
        chatRepository.deleteById(chatId);
    }

    @Transactional
    public MessageDTO sendGroupMessage(User sender, Long chatId, String content, Long replyToId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chat.getIsGroup()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This is not a group chat");
        }

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, sender.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        Message replyTo = resolveReplyTo(replyToId, chat);

        Message saved = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .replyTo(replyTo)
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
        HashMap<String, Object> payload = new HashMap<>();
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

    @Transactional
    public void deleteMessage(User user, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.getSender() == null || !message.getSender().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own messages");
        }

        if (message.isDeleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message is already deleted");
        }

        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        messageRepository.save(message);

        broadcastChatEvent(message.getChat().getId(), "MESSAGE_DELETED",
                Map.of("chatId", message.getChat().getId(), "messageId", messageId));
    }

    @Transactional
    public void editMessage(User user, Long messageId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.getSender() == null || !message.getSender().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only edit your own messages");
        }

        if (message.isDeleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot edit a deleted message");
        }

        if (message.isSystem()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot edit a system message");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        messageRepository.save(message);

        MessageDTO dto = toDTO(message);
        broadcastChatEvent(message.getChat().getId(), "MESSAGE_EDITED", dto);
    }

    @Transactional
    public void toggleReaction(User user, Long messageId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.isDeleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot react to a deleted message");
        }

        Optional<MessageReaction> existing = messageReactionRepository
                .findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), emoji);

        if (existing.isPresent()) {
            messageReactionRepository.delete(existing.get());
        } else {
            List<MessageReaction> userReactions = messageReactionRepository.findByMessageIdAndUserId(messageId, user.getId());
            if (!userReactions.isEmpty()) {
                messageReactionRepository.deleteAll(userReactions);
                messageReactionRepository.flush();
            }
            messageReactionRepository.save(MessageReaction.builder()
                    .message(message)
                    .user(user)
                    .emoji(emoji)
                    .build());
        }

        List<ReactionDTO> reactions = buildReactionsForMessage(messageId);
        broadcastChatEvent(message.getChat().getId(), "REACTION_UPDATED",
                Map.of("chatId", message.getChat().getId(), "messageId", messageId, "reactions", reactions));
    }

    @Transactional
    public void archiveChat(User user, Long chatId) {
        ChatParticipant cp = chatParticipantRepository.findByChatIdAndUserId(chatId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
        cp.setArchived(true);
        chatParticipantRepository.save(cp);
    }

    @Transactional
    public void unarchiveChat(User user, Long chatId) {
        ChatParticipant cp = chatParticipantRepository.findByChatIdAndUserId(chatId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
        cp.setArchived(false);
        chatParticipantRepository.save(cp);
    }

    @Transactional
    public void pinChat(User user, Long chatId) {
        ChatParticipant cp = chatParticipantRepository.findByChatIdAndUserId(chatId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
        cp.setPinned(true);
        cp.setPinnedAt(Instant.now());
        chatParticipantRepository.save(cp);
    }

    @Transactional
    public void unpinChat(User user, Long chatId) {
        ChatParticipant cp = chatParticipantRepository.findByChatIdAndUserId(chatId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
        cp.setPinned(false);
        cp.setPinnedAt(null);
        chatParticipantRepository.save(cp);
    }

    @Transactional
    public void deleteChatForUser(User user, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        log.info("deleteChatForUser: chatId={}, isGroup={}, partyLinked={}, userId={}",
                chatId, chat.getIsGroup(), chat.getPartyLinked(), user.getId());

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Chat not found");
        }

        if (Boolean.TRUE.equals(chat.getIsGroup())) {
            var partyOpt = partyRequestRepository.findByChatId(chatId);
            log.info("deleteChatForUser: partyOpt present={}, status={}",
                    partyOpt.isPresent(),
                    partyOpt.map(p -> p.getStatus().name()).orElse("N/A"));

            if (partyOpt.isPresent()) {
                var party = partyOpt.get();
                var status = party.getStatus();
                boolean partyActive = status == com.thespawnpoint.backend.entity.party.PartyStatus.OPEN
                        || status == com.thespawnpoint.backend.entity.party.PartyStatus.FULL
                        || status == com.thespawnpoint.backend.entity.party.PartyStatus.IN_GAME;
                if (partyActive) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Не можна вийти з чату поки лобі активне. Спочатку вийдіть з лобі.");
                }
            }

            sendSystemMessage(chat, user.getDisplayName() + " покинув(-ла) чат");
            chatParticipantRepository.deleteByChatIdAndUserId(chatId, user.getId());

            int remaining = chatParticipantRepository.countByChatId(chatId);
            if (remaining == 0) {
                List<String> emails = chatParticipantRepository.findByChatId(chatId).stream()
                        .map(cp -> cp.getUser().getEmail())
                        .collect(Collectors.toList());

                partyOpt.ifPresent(party -> {
                    party.setChat(null);
                    partyRequestRepository.save(party);
                });

                chatRepository.deleteById(chatId);

                ChatEventDTO event = new ChatEventDTO("CHAT_DELETED", chatId, Map.of("chatId", chatId));
                for (String email : emails) {
                    messagingTemplate.convertAndSendToUser(email, "/queue/chat-events", event);
                }
            }
        } else {
            ChatParticipant cp = chatParticipantRepository.findByChatIdAndUserId(chatId, user.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));
            cp.setDeletedAt(Instant.now());
            chatParticipantRepository.save(cp);

            List<ChatParticipant> all = chatParticipantRepository.findByChatId(chatId);
            boolean allDeleted = all.stream().allMatch(p -> p.getDeletedAt() != null);
            if (allDeleted) {
                chatRepository.deleteById(chatId);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> searchMessages(User user, Long chatId, String query, int page, int size) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        List<MessageDTO> messages = messageRepository
                .searchInChat(chat, query, PageRequest.of(page, size))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public void pinMessage(User user, Long chatId, Long messageId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getChat().getId().equals(chatId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message does not belong to this chat");
        }

        if (pinnedMessageRepository.existsByChatIdAndMessageId(chatId, messageId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Message is already pinned");
        }

        PinnedMessage pin = pinnedMessageRepository.save(PinnedMessage.builder()
                .chat(chat)
                .message(message)
                .pinnedBy(user)
                .build());

        PinnedMessageDTO dto = toPinnedDTO(pin);
        broadcastChatEvent(chatId, "MESSAGE_PINNED", dto);
        sendSystemMessage(chat, user.getDisplayName() + " pinned a message");
    }

    @Transactional
    public void unpinMessage(User user, Long chatId, Long messageId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chat not found"));

        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        if (!pinnedMessageRepository.existsByChatIdAndMessageId(chatId, messageId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Message is not pinned");
        }

        pinnedMessageRepository.deleteByChatIdAndMessageId(chatId, messageId);
        broadcastChatEvent(chatId, "MESSAGE_UNPINNED",
                Map.of("chatId", chatId, "messageId", messageId));
        sendSystemMessage(chat, user.getDisplayName() + " unpinned a message");
    }

    @Transactional(readOnly = true)
    public List<PinnedMessageDTO> getPinnedMessages(User user, Long chatId) {
        if (!chatParticipantRepository.existsByChatIdAndUserId(chatId, user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant of this chat");
        }

        return pinnedMessageRepository.findByChatId(chatId).stream()
                .map(this::toPinnedDTO)
                .toList();
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

    private void broadcastChatEvent(Long chatId, String type, Object payload) {
        ChatEventDTO event = new ChatEventDTO(type, chatId, payload);
        List<ChatParticipant> participants = chatParticipantRepository.findByChatId(chatId);
        for (ChatParticipant cp : participants) {
            messagingTemplate.convertAndSendToUser(
                    cp.getUser().getEmail(), "/queue/chat-events", event);
        }
    }

    @Transactional
    public List<ChatDTO> getUserChats(User currentUser) {
        return chatRepository.findAllByUser(currentUser).stream()
                .filter(chat -> {
                    return chatParticipantRepository.findByChatIdAndUserId(chat.getId(), currentUser.getId())
                            .map(cp -> cp.getDeletedAt() == null)
                            .orElse(false);
                })
                .map(chat -> buildChatDTO(chat, currentUser))
                .toList();
    }

    private ChatDTO buildChatDTO(Chat chat, User currentUser) {
        String lastMessage = null;
        Instant lastMessageAt = null;

        var lastMsgOpt = messageRepository.findFirstNonDeletedByChatOrderBySentAtDesc(chat, PageRequest.of(0, 1))
                .stream().findFirst();
        if (lastMsgOpt.isPresent()) {
            Message lm = lastMsgOpt.get();
            lastMessage = lm.getContent();
            lastMessageAt = lm.getSentAt();
        }

        int unread = messageRepository.countUnreadInChat(chat, currentUser);

        ChatParticipant currentCp = chatParticipantRepository.findByChatIdAndUserId(chat.getId(), currentUser.getId())
                .orElse(null);

        boolean archived = currentCp != null && currentCp.isArchived();
        boolean pinned = currentCp != null && currentCp.isPinned();
        Instant pinnedAt = currentCp != null ? currentCp.getPinnedAt() : null;

        if (Boolean.TRUE.equals(chat.getIsGroup())) {
            return buildGroupChatDTO(chat, currentUser, lastMessage, lastMessageAt, unread, archived, pinned, pinnedAt);
        } else {
            return buildDmChatDTO(chat, currentUser, lastMessage, lastMessageAt, unread, archived, pinned, pinnedAt);
        }
    }

    private ChatDTO buildDmChatDTO(Chat chat, User currentUser,
                                    String lastMessage, Instant lastMessageAt, int unread,
                                    boolean archived, boolean pinned, Instant pinnedAt) {
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
                .archived(archived)
                .pinned(pinned)
                .pinnedAt(pinnedAt)
                .build();
    }

    private ChatDTO buildGroupChatDTO(Chat chat, User currentUser,
                                       String lastMessage, Instant lastMessageAt, int unread,
                                       boolean archived, boolean pinned, Instant pinnedAt) {
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
                .archived(archived)
                .pinned(pinned)
                .pinnedAt(pinnedAt)
                .build();
    }

    private Long getChatIdIfExists(User sender, String recipientEmail) {
        return userRepository.findByEmail(recipientEmail)
                .flatMap(recipient -> chatRepository.findDmChat(sender, recipient))
                .map(Chat::getId)
                .orElse(null);
    }

    private Message resolveReplyTo(Long replyToId, Chat chat) {
        if (replyToId == null) return null;
        Message replyTo = messageRepository.findById(replyToId).orElse(null);
        if (replyTo != null && !replyTo.getChat().getId().equals(chat.getId())) {
            return null;
        }
        return replyTo;
    }

    private List<ReactionDTO> buildReactionsForMessage(Long messageId) {
        List<MessageReaction> reactions = messageReactionRepository.findByMessageId(messageId);
        Map<String, List<MessageReaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji));

        return grouped.entrySet().stream()
                .map(e -> ReactionDTO.builder()
                        .emoji(e.getKey())
                        .count(e.getValue().size())
                        .userEmails(e.getValue().stream()
                                .map(r -> r.getUser().getEmail())
                                .toList())
                        .build())
                .toList();
    }

    private PinnedMessageDTO toPinnedDTO(PinnedMessage pin) {
        Message msg = pin.getMessage();
        String senderName = msg.getSender() != null ? msg.getSender().getDisplayName() : "System";
        String content = msg.isDeleted() ? "Повідомлення видалено" : msg.getContent();
        if (content.length() > 100) content = content.substring(0, 100) + "…";

        return PinnedMessageDTO.builder()
                .id(pin.getId())
                .messageId(msg.getId())
                .chatId(pin.getChat().getId())
                .content(content)
                .senderName(senderName)
                .pinnedByName(pin.getPinnedBy().getDisplayName())
                .pinnedAt(pin.getPinnedAt())
                .build();
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

        Long replyToId = null;
        String replyToContent = null;
        String replyToSenderName = null;
        if (m.getReplyTo() != null) {
            Message reply = m.getReplyTo();
            replyToId = reply.getId();
            replyToSenderName = reply.getSender() != null ? reply.getSender().getDisplayName() : "System";
            if (reply.isDeleted()) {
                replyToContent = "Повідомлення видалено";
            } else {
                replyToContent = reply.getContent().length() > 100
                        ? reply.getContent().substring(0, 100) + "…"
                        : reply.getContent();
            }
        }

        List<ReactionDTO> reactions = buildReactionsForMessage(m.getId());

        String content = m.isDeleted() ? "Повідомлення видалено" : m.getContent();

        return MessageDTO.builder()
                .id(m.getId())
                .chatId(m.getChat().getId())
                .senderEmail(senderEmail)
                .senderName(senderName)
                .senderAvatarUrl(senderAvatarUrl)
                .content(content)
                .sentAt(m.getSentAt())
                .read(m.isRead())
                .system(m.isSystem())
                .deleted(m.isDeleted())
                .edited(m.isEdited())
                .editedAt(m.getEditedAt())
                .replyToId(replyToId)
                .replyToContent(replyToContent)
                .replyToSenderName(replyToSenderName)
                .reactions(reactions)
                .build();
    }
}
