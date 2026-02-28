package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.ChatDTO;
import com.thespawnpoint.backend.dto.MessageDTO;
import com.thespawnpoint.backend.dto.SendMessageDTO;
import com.thespawnpoint.backend.dto.TypingDTO;
import com.thespawnpoint.backend.entity.User;
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

    @GetMapping("/api/chats")
    public ResponseEntity<List<ChatDTO>> getChats(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getUserChats(currentUser));
    }

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

    private User getPrincipalUser(Principal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
