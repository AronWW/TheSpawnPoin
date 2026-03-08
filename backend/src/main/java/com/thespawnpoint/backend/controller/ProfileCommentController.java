package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.ProfileCommentDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.ProfileCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile/{userId}/comments")
@RequiredArgsConstructor
public class ProfileCommentController {

    private final ProfileCommentService commentService;

    @GetMapping
    public ResponseEntity<Page<ProfileCommentDTO>> getComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.getComments(userId, page, Math.min(size, 50)));
    }

    @PostMapping
    public ResponseEntity<ProfileCommentDTO> addComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "");
        return ResponseEntity.ok(commentService.addComment(user, userId, content));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        commentService.deleteComment(user, commentId);
        return ResponseEntity.noContent().build();
    }
}

