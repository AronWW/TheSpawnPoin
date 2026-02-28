package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.ChatDTO;
import com.thespawnpoint.backend.dto.CreateGroupChatDTO;
import com.thespawnpoint.backend.dto.MessageDTO;
import com.thespawnpoint.backend.dto.SendGroupMessageDTO;
import com.thespawnpoint.backend.dto.SendMessageDTO;
import com.thespawnpoint.backend.dto.TypingDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.exception.WebSocketExceptionHandler;
import com.thespawnpoint.backend.repository.UserRepository;
import com.thespawnpoint.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController extends WebSocketExceptionHandler {

    private final ChatService chatService;
    private final UserRepository userRepository;

    // ======================== CHAT LIST ========================

    @GetMapping("/api/chats")
    public ResponseEntity<List<ChatDTO>> getChats(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getUserChats(currentUser));
    }

    // ======================== DM ========================

    @GetMapping("/api/chats/{partnerEmail}/messages")
    public ResponseEntity<List<MessageDTO>> history(
            @PathVariable String partnerEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(chatService.getHistory(currentUser, partnerEmail, page, size));
    }

    @PostMapping("/api/chats/dm/{partnerEmail}")
    public ResponseEntity<Map<String, Long>> openDmChat(
            @PathVariable String partnerEmail,
            @AuthenticationPrincipal User currentUser) {

        User partner = userRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Long chatId = chatService.getOrCreateDmChat(currentUser, partner).getId();
        return ResponseEntity.ok(Map.of("chatId", chatId));
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageDTO dto, Principal principal) {
        User sender = getPrincipalUser(principal);
        chatService.sendMessage(sender, dto.getRecipientEmail(), dto.getContent());
        log.debug("Message from {} to {}", sender.getEmail(), dto.getRecipientEmail());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingDTO dto, Principal principal) {
        chatService.sendTypingIndicator(getPrincipalUser(principal), dto.getRecipientEmail());
    }

    @MessageMapping("/chat.read")
    public void markRead(@Payload Map<String, String> payload, Principal principal) {
        String senderEmail = payload.get("senderEmail");
        if (senderEmail == null) return;
        chatService.markAsReadAndNotify(getPrincipalUser(principal), senderEmail);
    }

    // ======================== GROUP CHAT ========================

    @PostMapping("/api/chats/group")
    public ResponseEntity<ChatDTO> createGroupChat(
            @RequestBody @jakarta.validation.Valid CreateGroupChatDTO dto,
            @AuthenticationPrincipal User currentUser) {
        ChatDTO chat = chatService.createStandaloneGroupChat(currentUser, dto.getTitle(), dto.getMemberEmails());
        return ResponseEntity.status(HttpStatus.CREATED).body(chat);
    }

    @PostMapping("/api/chats/group/{chatId}/members")
    public ResponseEntity<Void> addGroupChatMember(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        chatService.addGroupChatParticipant(currentUser, chatId, email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chats/group/{chatId}/leave")
    public ResponseEntity<Void> leaveGroupChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        chatService.leaveGroupChat(currentUser, chatId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/chats/group/{chatId}/title")
    public ResponseEntity<ChatDTO> renameGroupChat(
            @PathVariable Long chatId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        return ResponseEntity.ok(chatService.renameGroupChat(currentUser, chatId, title));
    }

    @GetMapping("/api/chats/group/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> groupHistory(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(chatService.getGroupHistory(currentUser, chatId, page, size));
    }

    @MessageMapping("/chat.sendGroup")
    public void sendGroupMessage(@Payload SendGroupMessageDTO dto, Principal principal) {
        User sender = getPrincipalUser(principal);
        chatService.sendGroupMessage(sender, dto.getChatId(), dto.getContent());
        log.debug("Group message from {} to chat {}", sender.getEmail(), dto.getChatId());
    }

    @MessageMapping("/chat.typingGroup")
    public void typingGroup(@Payload Map<String, Object> payload, Principal principal) {
        Long chatId = ((Number) payload.get("chatId")).longValue();
        chatService.sendGroupTypingIndicator(getPrincipalUser(principal), chatId);
    }

    @MessageMapping("/chat.readGroup")
    public void markGroupRead(@Payload Map<String, Object> payload, Principal principal) {
        Long chatId = ((Number) payload.get("chatId")).longValue();
        chatService.markGroupAsRead(getPrincipalUser(principal), chatId);
    }

    // ======================== HELPER ========================

    private User getPrincipalUser(Principal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
